package com.tfdsimoes.checkers.game;

public final class UndoPolicy {
    private UndoPolicy() {
    }

    public static int movesToUndo(GameMode mode, int historySize) {
        if (historySize < 0) {
            throw new IllegalArgumentException("History size cannot be negative");
        }
        return switch (mode) {
            case PLAYER_VS_PLAYER -> Math.min(1, historySize);
            case PLAYER_VS_MACHINE -> Math.min(2, historySize);
            case MACHINE_VS_MACHINE -> 0;
        };
    }
}
