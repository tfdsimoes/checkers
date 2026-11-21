package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.model.Side;

public final class UndoPolicy {
    private UndoPolicy() {
    }

    public static int movesToUndo(GameMode mode, int historySize, Side humanSide) {
        if (historySize < 0) {
            throw new IllegalArgumentException("History size cannot be negative");
        }
        return switch (mode) {
            case PLAYER_VS_PLAYER -> Math.min(1, historySize);
            case PLAYER_VS_MACHINE -> movesSinceLastHumanPly(historySize, humanSide);
            case MACHINE_VS_MACHINE -> 0;
        };
    }

    private static int movesSinceLastHumanPly(int historySize, Side humanSide) {
        if (humanSide == null) {
            throw new IllegalArgumentException("Human side is required for player-versus-machine undo");
        }
        int lastPly = historySize - 1;
        int humanPlyParity = humanSide == Side.BLACK ? 0 : 1;
        int lastHumanPly = lastPly - (Math.floorMod(lastPly, 2) == humanPlyParity ? 0 : 1);
        return lastHumanPly < 0 ? 0 : historySize - lastHumanPly;
    }
}
