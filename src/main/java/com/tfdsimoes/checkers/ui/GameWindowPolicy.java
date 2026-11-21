package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.game.GameStatus;

final class GameWindowPolicy {
    private GameWindowPolicy() {
    }

    static boolean showDrawResponseButtons(
            GameMode mode, GameStatus status, boolean offerPending, boolean responseReady) {
        return mode == GameMode.PLAYER_VS_PLAYER
                && status == GameStatus.IN_PROGRESS
                && offerPending
                && responseReady;
    }

    static boolean canStartAiTurn(
            boolean returning,
            boolean paused,
            GameStatus status,
            boolean workerRunning,
            boolean currentPlayerIsHuman) {
        return !returning && !paused && status == GameStatus.IN_PROGRESS
                && !workerRunning && !currentPlayerIsHuman;
    }
}
