package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.studio.model.UndoRedoHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoRedoHistoryTest {

    @Test
    void supportsMultiStepUndoRedoAndClearsRedoOnNewBranch() {
        UndoRedoHistory<String> history = new UndoRedoHistory<>(50);
        String state = "v1";

        history.push(state);
        state = "v2";
        history.push(state);
        state = "v3";

        assertTrue(history.canUndo());
        assertFalse(history.canRedo());

        state = history.undo(state);
        assertEquals("v2", state);
        assertTrue(history.canRedo());

        state = history.undo(state);
        assertEquals("v1", state);

        state = history.redo(state);
        assertEquals("v2", state);

        // New edit after undo must invalidate redo branch
        history.push(state);
        state = "v4";
        assertFalse(history.canRedo());
        assertNull(history.redo(state));
    }
}
