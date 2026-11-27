package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.studio.model.UndoStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoStackTest {

    @Test
    void retainsAtLeastTwentyStepsAndDropsOldestBeyondCapacity() {
        UndoStack<Integer> stack = new UndoStack<>(25);
        for (int i = 1; i <= 30; i++) {
            stack.push(i);
        }
        assertEquals(25, stack.size());
        assertTrue(stack.canUndo());

        assertEquals(30, stack.undo());
        assertEquals(29, stack.undo());

        for (int i = 0; i < 23; i++) {
            stack.undo();
        }
        assertFalse(stack.canUndo());
        assertNull(stack.undo());
    }
}
