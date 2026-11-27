package com.minhtriet79.goldminer.studio.model;

import java.util.ArrayDeque;

/**
 * Bounded Undo/Redo history supporting bidirectional state traversal and branching.
 */
public class UndoRedoHistory<T> {

    public static final int DEFAULT_MAX_STEPS = 50;

    private final int maxSteps;
    private final ArrayDeque<T> undoStack = new ArrayDeque<>();
    private final ArrayDeque<T> redoStack = new ArrayDeque<>();

    public UndoRedoHistory() {
        this(DEFAULT_MAX_STEPS);
    }

    public UndoRedoHistory(int maxSteps) {
        this.maxSteps = Math.max(20, maxSteps);
    }

    public void push(T snapshotBeforeEdit) {
        if (snapshotBeforeEdit == null) {
            return;
        }
        if (undoStack.size() >= maxSteps) {
            undoStack.removeLast();
        }
        undoStack.push(snapshotBeforeEdit);
        redoStack.clear();
    }

    public T undo(T currentStateSnapshot) {
        if (undoStack.isEmpty()) {
            return null;
        }
        if (currentStateSnapshot != null) {
            if (redoStack.size() >= maxSteps) {
                redoStack.removeLast();
            }
            redoStack.push(currentStateSnapshot);
        }
        return undoStack.pop();
    }

    public T redo(T currentStateSnapshot) {
        if (redoStack.isEmpty()) {
            return null;
        }
        if (currentStateSnapshot != null) {
            if (undoStack.size() >= maxSteps) {
                undoStack.removeLast();
            }
            undoStack.push(currentStateSnapshot);
        }
        return redoStack.pop();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public int undoSize() {
        return undoStack.size();
    }

    public int redoSize() {
        return redoStack.size();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
