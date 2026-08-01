package pl.olafcio.accesseditors.keyword.impl;

import org.jetbrains.annotations.Range;
import org.objectweb.asm.tree.ClassNode;
import pl.olafcio.accesseditors.error.IllegalKeywordException;
import pl.olafcio.accesseditors.error.NeedsAbstractException;
import pl.olafcio.accesseditors.keyword.Modifiers;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public enum ClassKeyword {
    PUBLIC(node -> {
        node.access = Modifiers.unapply(node.access, ACC_PRIVATE);
        node.access = Modifiers.unapply(node.access, ACC_PROTECTED);
        node.access = Modifiers.apply(node.access, ACC_PUBLIC);
    }, false),
    PROTECTED(node -> {
        if (!Modifiers.has(node.access, ACC_PUBLIC)) {
            node.access = Modifiers.apply(node.access, ACC_PROTECTED);
            node.access = Modifiers.unapply(node.access, ACC_PRIVATE);
        }
    }, false),
    PRIVATE(ACC_PRIVATE, false, false),
    PACKAGE_PRIVATE(),
    ABSTRACT(false, node -> {
        for (var m : node.methods)
            if (Modifiers.has(m.access, ACC_ABSTRACT))
                throw new NeedsAbstractException("Cannot remove 'abstract' from a " + getType(node) + "s");

        node.access = Modifiers.unapply(node.access, ACC_ABSTRACT);
    }),
    STATIC(),
    SEALED(false, node -> {
        node.permittedSubclasses = null;
    }),
    NON_SEALED(),
    FINAL(ACC_FINAL, false, true),
    TRANSIENT,
    VOLATILE(),
    SYNCHRONIZED(),
    STRICTFP();

    private final int access;
    private final Function<ClassNode, Boolean> canAdd;
    private final Function<ClassNode, Boolean> canRemove;

    ClassKeyword() {
        this.access = -1;
        this.canAdd = _ -> false;
        this.canRemove = _ -> false;
    }

    ClassKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, boolean addable, boolean removable) {
        this.access = access;
        this.canAdd = _ -> addable;
        this.canRemove = _ -> removable;
    }

    ClassKeyword(Consumer<ClassNode> add, boolean removable) {
        this.access = -1;
        this.canAdd = node -> {
            add.accept(node);
            return null;
        };
        this.canRemove = _ -> removable;
    }

    ClassKeyword(boolean addable, Consumer<ClassNode> remove) {
        this.access = -1;
        this.canAdd = _ -> addable;
        this.canRemove = node -> {
            remove.accept(node);
            return null;
        };
    }

    public void add(ClassNode node) {
        var value = this.canAdd.apply(node);
        if (value == null)
            return;
        else if (!value)
            throw new IllegalKeywordException("'%s' keyword cannot be added to %s".formatted(this.name(), getType(node) + "s"));

        node.access |= access;
    }

    public void remove(ClassNode node) {
        var value = this.canRemove.apply(node);
        if (value == null)
            return;
        else if (!value)
            throw new IllegalKeywordException("'%s' keyword cannot be removed from %s".formatted(this.name(), getType(node) + "s"));

        if ((node.access & access) == access)
            node.access -= access;
    }

    public void toggle(ClassNode node, boolean value) {
        if (value)
            add(node);
        else remove(node);
    }

    private static String getType(ClassNode node) {
        return ((node.access & ACC_ENUM)       == ACC_ENUM)       ? "enum"       :
               ((node.access & ACC_RECORD)     == ACC_RECORD)     ? "record"     :
               ((node.access & ACC_INTERFACE)  == ACC_INTERFACE)  ? "interface"  :
               ((node.access & ACC_ANNOTATION) == ACC_ANNOTATION) ? "@interface" :
                                                                    "class";
    }
}
