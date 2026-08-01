package pl.olafcio.accesseditors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import pl.olafcio.accesseditors.error.SyntaxException;
import pl.olafcio.accesseditors.file.Mode;
import pl.olafcio.accesseditors.file.Modification;
import pl.olafcio.accesseditors.file.Properties;
import pl.olafcio.accesseditors.keyword.impl.ClassKeyword;
import pl.olafcio.accesseditors.keyword.impl.FieldKeyword;
import pl.olafcio.accesseditors.keyword.impl.MethodKeyword;
import pl.olafcio.accesseditors.lang.Lexer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * The class used to load access editors in a java agent.
 */
@NullMarked
public class AccessEditorLoader {
    // key = class internal name
    protected final HashMap<String, ArrayList<Modification>> modifications
              = new HashMap<>();

    /**
     * Parses the provided access editor and saves its goals.
     * @param accessEditor The access editor text content.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void add(String accessEditor) {
        var lexer = new Lexer(accessEditor);

        lexer.expect("accesseditor v1");

        var properties = new Properties();

        while (!lexer.reachedEOF())
        {
            if (lexer.now(' '));
            else if (lexer.now('\n'));
            else if (lexer.now("#")) {
                lexer.until('\n');
            } else if (lexer.now("[")) {
                lexer.expect('"');

                var key = lexer.until('"');
                lexer.expect(' ');
                var value = lexer.until(']');

                properties.put(key, value);

                while (lexer.now(' '));
                if (lexer.now("#"))
                    lexer.until('\n');

                lexer.expect('\n');
            } else if (lexer.now("implement")) {
                lexer.expect(' ');

                var className = lexer.until(' ');
                lexer.expect(" += ");
                var interfaceName = lexer.until('\n');

                modifications.computeIfAbsent(className, _ -> new ArrayList<>())
                             .add(new Modification.ClassImplement(interfaceName));
            } else {
                modification:
                {
                    Mode mode;

                    if (lexer.now("+"))
                        mode = Mode.ADD;
                    else if (lexer.now("-"))
                        mode = Mode.REMOVE;
                    else break modification;

                    var keyword = lexer.until(' ');
                    if (keyword.contains("_"))
                        throw new SyntaxException("Keyword contains underscores (_); did you confuse it with the class or field name?");

                    while (lexer.now(' '));

                    var elementType = lexer.until(' ');
                    while (lexer.now(' '));

                    var classname = lexer.until(' ');
                    while (lexer.now(' '));

                    if (classname.contains("."))
                        throw new SyntaxException("Type name contains dots (.); it should be separated by slashes (/)");

                    IO.println("[AccessEditors] Deferring %s %s %s".formatted(mode.name().toLowerCase(), elementType, keyword));

                    Supplier<String> fourthgetter = () -> {
                        var value = new StringBuilder();

                        while (!lexer.reachedEOF()) {
                            if (lexer.future('\n') || lexer.future(' '))
                                break;

                            value.appendCodePoint(lexer.consume());
                        }

                        return value.toString();
                    };

                    switch (elementType) {
                        case "class" -> modifications.computeIfAbsent(classname, _ -> new ArrayList<>())
                                                     .add(new Modification.ClassKW(
                                                             ClassKeyword.valueOf(keyword.toUpperCase().replace("-", "_")),
                                                             mode
                                                     ));

                        case "method" -> modifications.computeIfAbsent(classname, _ -> new ArrayList<>())
                                                      .add(new Modification.MethodKW(
                                                              MethodKeyword.valueOf(keyword.toUpperCase().replace("-", "_")),
                                                              mode,
                                                              fourthgetter.get()
                                                      ));

                        case "field" -> modifications.computeIfAbsent(classname, _ -> new ArrayList<>())
                                                     .add(new Modification.FieldKW(
                                                             FieldKeyword.valueOf(keyword.toUpperCase().replace("-", "_")),
                                                             mode,
                                                             fourthgetter.get()
                                                     ));

                        default ->
                                throw new SyntaxException("Expected class, method or field; got '%s'".formatted(elementType));
                    }

                    while (lexer.now(' '));
                    if (lexer.now("#"))
                        lexer.until('\n');

                    if (!lexer.reachedEOF())
                        lexer.expect('\n');

                    continue;
                }

                throw new SyntaxException("Unexpected '%c'".formatted(lexer.peek(0)));
            }
        }
    }

    /**
     * Registers a classtransformer for all the modifications.
     */
    public void apply(Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            @NullUnmarked
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (modifications.containsKey(className)) {
                    var reader = new ClassReader(classfileBuffer);
                    var node = new ClassNode();

                    reader.accept(node, Opcodes.ASM9);

                    try {
                        var mods = modifications.get(className);

                        for (Modification mod : mods) {
                            switch (mod) {
                                case Modification.ClassKW(ClassKeyword keyword, Mode mode) ->
                                        keyword.toggle(node, mode == Mode.ADD);

                                case Modification.FieldKW(FieldKeyword keyword, Mode mode, String fieldName) ->
                                        keyword.toggle(node.fields.stream()
                                                                  .filter(f -> f.name.equals(fieldName))
                                                                  .findAny()
                                                                  .orElseThrow(), mode == Mode.ADD);

                                case Modification.MethodKW(MethodKeyword keyword, Mode mode, String signature) ->
                                        keyword.toggle(node.methods.stream()
                                                                   .filter(f -> (f.name + f.desc).equals(signature))
                                                                   .findAny()
                                                                   .orElseThrow(), mode == Mode.ADD);

                                case Modification.ClassImplement(String interfacePath) ->
                                        node.interfaces.add(interfacePath);

                                default ->
                                        throw new UnsupportedOperationException("Unsupported operation %s".formatted(mod));
                            }
                        }
                    } catch (Exception e) {
                        IO.println("[AccessEditors] !!!!!!!!!!!!!!!!!!!!!!");
                        IO.println("[AccessEditors] !!!!!!!!!!!!!!!!!!!!!!");
                        IO.println("[AccessEditors] Unable to transform %s".formatted(className));
                        IO.println("[AccessEditors] !!!!!!!!!!!!!!!!!!!!!!");
                        IO.println("[AccessEditors] !!!!!!!!!!!!!!!!!!!!!!");
                        IO.println("[AccessEditors] ");

                        e.printStackTrace();

                        System.exit(1);
                    }

                    var writer = new ClassWriter(0);
                    node.accept(writer);
                    return writer.toByteArray();
                }

                return null;
            }
        });
    }
}
