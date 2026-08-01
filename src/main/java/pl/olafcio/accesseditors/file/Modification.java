package pl.olafcio.accesseditors.file;

import pl.olafcio.accesseditors.keyword.impl.ClassKeyword;
import pl.olafcio.accesseditors.keyword.impl.FieldKeyword;
import pl.olafcio.accesseditors.keyword.impl.MethodKeyword;

public sealed interface Modification {
    record ClassKW(ClassKeyword keyword, Mode mode) implements Modification {}
    record FieldKW(FieldKeyword keyword, Mode mode, String name) implements Modification {}
    record MethodKW(MethodKeyword keyword, Mode mode, String signature) implements Modification {}

    record ClassImplement(String interfacePath) implements Modification {}
}
