package pl.olafcio.accesseditors.keyword;

import org.intellij.lang.annotations.MagicConstant;
import org.objectweb.asm.Opcodes;

@SuppressWarnings("MagicConstant")
public enum Modifiers {
    ;

    public static int apply(int flags, @MagicConstant(valuesFromClass = Opcodes.class) int modifier) {
        flags |= modifier;

        return flags;
    }

    public static int unapply(int flags, @MagicConstant(valuesFromClass = Opcodes.class) int modifier) {
        if ((flags & modifier) == modifier)
            flags -= modifier;

        return flags;
    }

    public static boolean has(int flags, @MagicConstant(valuesFromClass = Opcodes.class) int modifier) {
        return ((flags & modifier) == modifier);
    }
}
