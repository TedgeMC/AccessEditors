package pl.olafcio.accesseditors.keyword.impl;

import org.jetbrains.annotations.Range;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import pl.olafcio.accesseditors.error.IllegalKeywordException;
import pl.olafcio.accesseditors.keyword.Modifiers;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public enum MethodKeyword {
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
    TRANSIENT(),
    VOLATILE(),
    SYNCHRONIZED(ACC_SYNCHRONIZED, true, true),
    STRICTFP(ACC_STRICT, true, false);

    private final int access;
    private final Function<MethodNode, Boolean> canAdd;
    private final Function<MethodNode, Boolean> canRemove;

    MethodKeyword() {
        this.access = -1;
        this.canAdd = _ -> false;
        this.canRemove = _ -> false;
    }

    MethodKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, boolean addable, boolean removable) {
        this.access = access;
        this.canAdd = _ -> addable;
        this.canRemove = _ -> removable;
    }

    MethodKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, Consumer<MethodNode> add, boolean removable) {
        this.access = access;
        this.canAdd = node -> {
            add.accept(node);
            return null;
        };
        this.canRemove = _ -> removable;
    }

    MethodKeyword(@Range(from = 0, to = Integer.MAX_VALUE) int access, boolean addable, Consumer<MethodNode> remove) {
        this.access = access;
        this.canAdd = _ -> addable;
        this.canRemove = node -> {
            remove.accept(node);
            return null;
        };
    }

    public void add(MethodNode node) {
        var value = this.canAdd.apply(node);
        if (value == null)
            return;
        else if (!value)
            throw new IllegalKeywordException("'%s' keyword cannot be added to methods".formatted(this.name()));

        node.access |= access;
    }

    public void remove(MethodNode node) {
        var value = this.canRemove.apply(node);
        if (value == null)
            return;
        else if (!value)
            throw new IllegalKeywordException("'%s' keyword cannot be removed from methods".formatted(this.name()));

        if ((node.access & access) == access)
            node.access -= access;
    }

    public void toggle(MethodNode node, boolean value) {
        if (value)
            add(node);
        else remove(node);
    }
}
