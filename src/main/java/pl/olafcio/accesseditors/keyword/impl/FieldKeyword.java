package pl.olafcio.accesseditors.keyword.impl;

import org.jetbrains.annotations.Range;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import pl.olafcio.accesseditors.error.IllegalKeywordException;
import pl.olafcio.accesseditors.error.NeedsAbstractException;
import pl.olafcio.accesseditors.keyword.Modifiers;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public enum FieldKeyword {
    PUBLIC(ACC_PUBLIC, node -> {
        node.access = Modifiers.unapply(node.access, ACC_PRIVATE);
        node.access = Modifiers.unapply(node.access, ACC_PROTECTED);
        node.access = Modifiers.apply(node.access, ACC_PUBLIC);
    }, false),
    PROTECTED(ACC_PROTECTED, node -> {
        if (!Modifiers.has(node.access, ACC_PUBLIC)) {
            node.access = Modifiers.apply(node.access, ACC_PROTECTED);
            node.access = Modifiers.unapply(node.access, ACC_PRIVATE);
        }
    }, false),
    PRIVATE(ACC_PRIVATE, false, false),
    PACKAGE_PRIVATE(),
    ABSTRACT(),
    STATIC(ACC_STATIC, false, false),
    SEALED(),
    NON_SEALED(),
    FINAL(ACC_FINAL, false, true),
    TRANSIENT(ACC_TRANSIENT, true, true),
    VOLATILE(ACC_VOLATILE, true, true),
    SYNCHRONIZED(ACC_SYNCHRONIZED, true, true),
    STRICTFP(ACC_STRICT, true, false);

    private final int access;
    private final Function<FieldNode, Boolean> canAdd;
    private final Function<FieldNode, Boolean> canRemove;

    FieldKeyword() {
        this.access = -1;
        this.canAdd = _ -> false;
        this.canRemove = _ -> false;
    }

    FieldKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, boolean addable, boolean removable) {
        this.access = access;
        this.canAdd = _ -> addable;
        this.canRemove = _ -> removable;
    }

    FieldKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, Consumer<FieldNode> add, boolean removable) {
        this.access = access;
        this.canAdd = node -> {
            add.accept(node);
            return null;
        };
        this.canRemove = _ -> removable;
    }

    FieldKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, boolean addable, Consumer<FieldNode> remove) {
        this.access = access;
        this.canAdd = _ -> addable;
        this.canRemove = node -> {
            remove.accept(node);
            return null;
        };
    }

    public void add(FieldNode node) {
        var value = this.canAdd.apply(node);
        if (value == null)
            return;
        else if (!value)
            throw new IllegalKeywordException("'%s' keyword cannot be added to fields".formatted(this.name()));

        node.access |= access;
    }

    public void remove(FieldNode node) {
        var value = this.canRemove.apply(node);
        if (value == null)
            return;
        else if (!value)
            throw new IllegalKeywordException("'%s' keyword cannot be removed from fields".formatted(this.name()));

        if ((node.access & access) == access)
            node.access -= access;
    }

    public void toggle(FieldNode node, boolean value) {
        if (value)
            add(node);
        else remove(node);
    }
}
