package pl.olafcio.accesseditors.file;

import pl.olafcio.accesseditors.keyword.impl.ClassKeyword;
import pl.olafcio.accesseditors.keyword.impl.FieldKeyword;
import pl.olafcio.accesseditors.keyword.impl.MethodKeyword;

public sealed interface Modification {
    record ClassKW(ClassKeyword keyword) implements Modification {}
    record FieldKW(FieldKeyword keyword, String name) implements Modification {}
    record MethodKW(MethodKeyword keyword, String signature) implements Modification {}
}
