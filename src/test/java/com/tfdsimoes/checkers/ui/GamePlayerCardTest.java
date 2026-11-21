package com.tfdsimoes.checkers.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.ai.AlgorithmRegistry;
import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.game.AiController;
import com.tfdsimoes.checkers.game.HumanController;
import com.tfdsimoes.checkers.model.Side;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class GamePlayerCardTest {
    @Test
    void summarizesHumanPlayerAndTurnIndicator() {
        GamePlayerCard.CardDetails details = GamePlayerCard.summarize(
                Side.BLACK, new HumanController("Player 1"), true, "TO MOVE");

        assertEquals("Black", details.sideName());
        assertEquals("Player 1", details.playerName());
        assertEquals("Human", details.algorithm());
        assertEquals("TO MOVE", details.turnStatus());
        assertTrue(details.active());
    }

    @Test
    void summarizesMachineNameAndSelectedAlgorithm() {
        var strategy = new AlgorithmRegistry().strategies(Difficulty.EASY).get(0);
        GamePlayerCard.CardDetails details = GamePlayerCard.summarize(
                Side.WHITE, new AiController(strategy, new SplittableRandom(17)), false, "WAITING");

        assertEquals("White", details.sideName());
        assertEquals("Machine", details.playerName());
        assertEquals(strategy.name(), details.algorithm());
        assertEquals("WAITING", details.turnStatus());
    }
}
