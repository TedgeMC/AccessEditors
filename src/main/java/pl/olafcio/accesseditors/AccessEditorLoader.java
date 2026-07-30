package pl.olafcio.accesseditors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static pl.olafcio.accesseditors.util.StringListUtil.*;

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
    public void add(String accessEditor) {
        var linesraw = accessEditor.replace("\r", "\n").replace("\n\n", "\n").split("\n");
        var lines = Arrays.asList(linesraw);

        expect(lines, "accesseditor v1");

        var properties = new Properties();

        while (!lines.isEmpty())
        {
            var orig = lines.getFirst();
            var line = orig.stripIndent();

            if (line.isEmpty())
            {
                lines.removeFirst();
            }
            else if (line.startsWith("["))
            {
                skip(lines, 1 + (orig.length() - line.length()));
                expect(lines, '"');

                var key = until(lines, '"');
                expect(lines, ' ');
                var value = until(lines, ']');

                properties.put(key, value);

                expect(lines, '\n');
            }
            else if (line.startsWith("#"))
            {
                lines.removeFirst();
            }
            else
            {
                modification:
                {
                    Mode mode;

                    if (line.startsWith("+"))
                        mode = Mode.ADD;
                    else if (line.startsWith("-"))
                        mode = Mode.REMOVE;
                    else break modification;

                    line = line.substring(1);

                    var words = line.stripTrailing().split(" ");

                    var keyword = words[0];
                    if (keyword.contains("_"))
                        throw new SyntaxException("Keyword contains underscores (_); did you confuse it with the class or field name?");

                    var elementType = words[1];
                    var elementWords = elementType.equals("class") ? 3 : 4;

                    if (words.length != elementWords)
                        throw new SyntaxException("Expected %d words, got %d".formatted(elementWords, words.length));

                    var classname = words[2];
                    if (classname.contains("."))
                        throw new SyntaxException("Type name contains dots (.); it should be separated by slashes (/)");

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
                                                              words[3]
                                                      ));

                        case "field" -> modifications.computeIfAbsent(classname, _ -> new ArrayList<>())
                                                     .add(new Modification.FieldKW(
                                                             FieldKeyword.valueOf(keyword.toUpperCase().replace("-", "_")),
                                                             mode,
                                                             words[3]
                                                     ));

                        default ->
                                throw new SyntaxException("Expected class, method or field; got '%s'".formatted(elementType));
                    }

                    continue;
                }

                throw new SyntaxException("Unexpected '%s'".formatted(line));
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

                            default ->
                                    throw new UnsupportedOperationException("Unsupported operation %s".formatted(mod));
                        }
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
