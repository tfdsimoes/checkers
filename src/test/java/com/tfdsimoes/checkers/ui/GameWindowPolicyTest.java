package com.tfdsimoes.checkers.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.game.GameStatus;
import org.junit.jupiter.api.Test;

class GameWindowPolicyTest {
    @Test
    void drawResponseControlsAreOnlyShownForAnActivePlayerVersusPlayerOffer() {
        assertFalse(GameWindowPolicy.showDrawResponseButtons(
                GameMode.PLAYER_VS_PLAYER, GameStatus.IN_PROGRESS, false, false));
        assertTrue(GameWindowPolicy.showDrawResponseButtons(
                GameMode.PLAYER_VS_PLAYER, GameStatus.IN_PROGRESS, true, true));
        assertFalse(GameWindowPolicy.showDrawResponseButtons(
                GameMode.PLAYER_VS_PLAYER, GameStatus.IN_PROGRESS, true, false));
        assertFalse(GameWindowPolicy.showDrawResponseButtons(
                GameMode.PLAYER_VS_MACHINE, GameStatus.IN_PROGRESS, true, true));
        assertFalse(GameWindowPolicy.showDrawResponseButtons(
                GameMode.PLAYER_VS_PLAYER, GameStatus.DRAW, true, true));
    }

    @Test
    void pausingBlocksAiAndResumingAllowsItsTurn() {
        assertFalse(GameWindowPolicy.canStartAiTurn(
                false, true, GameStatus.IN_PROGRESS, false, false));
        assertTrue(GameWindowPolicy.canStartAiTurn(
                false, false, GameStatus.IN_PROGRESS, false, false));
    }
}
