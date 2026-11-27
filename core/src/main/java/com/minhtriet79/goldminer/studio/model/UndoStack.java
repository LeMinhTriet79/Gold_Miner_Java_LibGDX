package com.minhtriet79.goldminer.studio.model;

import java.util.ArrayDeque;

/**
 * Fixed-capacity undo stack retaining at least 20 editor state snapshots.
 */
public class UndoStack<T> {

    public static final int DEFAULT_MAX_STEPS = 30;

    private final int maxSteps;
    private final ArrayDeque<T> stack = new ArrayDeque<>();

    public UndoStack() {
        this(DEFAULT_MAX_STEPS);
    }

    public UndoStack(int maxSteps) {
        this.maxSteps = Math.max(20, maxSteps);
    }

    public void push(T snapshot) {
        if (snapshot == null) {
            return;
        }
        if (stack.size() >= maxSteps) {
            stack.removeLast();
        }
        stack.push(snapshot);
    }

    public T undo() {
        return stack.isEmpty() ? null : stack.pop();
    }

    public boolean canUndo() {
        return !stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        stack.clear();
    }
}
