package com.tfdsimoes.checkers.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class HardVsMediumStrengthTest {
    private static final int GAMES = 20;
    private static final int MINIMUM_WINS = 12;
    private static final int MAXIMUM_LOSSES = 4;

    @Test
    void hardPoolMeetsTheSeededWinRateTargetAgainstTheMediumPool() {
        AlgorithmRegistry registry = new AlgorithmRegistry();
        int wins = 0;
        int losses = 0;
        int draws = 0;
        for (int gameIndex = 0; gameIndex < GAMES; gameIndex++) {
            long seed = 0x5EEDL + gameIndex;
            AiStrategy hard = registry.select(
                    Difficulty.HARD, new SplittableRandom(seed ^ 0xA11CE));
            AiStrategy medium = registry.select(
                    Difficulty.MEDIUM, new SplittableRandom(seed ^ 0xC0FFEE));
            Side hardSide = gameIndex % 2 == 0 ? Side.BLACK : Side.WHITE;
            GameStatus result = playGame(
                    hard,
                    medium,
                    hardSide,
                    new SplittableRandom(seed ^ 0xBADC0DE),
                    new SplittableRandom(seed ^ 0x5EED5EED));
            if (isWinFor(result, hardSide)) {
                wins++;
            } else if (result == GameStatus.DRAW) {
                draws++;
            } else {
                losses++;
            }
        }
        System.out.printf("Hard vs Medium: %d wins, %d losses, %d draws%n", wins, losses, draws);
        assertTrue(wins >= MINIMUM_WINS,
                "Hard won " + wins + " of " + GAMES + " games");
        assertTrue(losses <= MAXIMUM_LOSSES,
                "Hard lost " + losses + " of " + GAMES + " games");
    }

    private static GameStatus playGame(
            AiStrategy hard,
            AiStrategy medium,
            Side hardSide,
            SplittableRandom hardRandom,
            SplittableRandom mediumRandom) {
        GameState game = new GameState();
        while (game.status() == GameStatus.IN_PROGRESS) {
            boolean hardToMove = game.currentPlayer() == hardSide;
            AiStrategy strategy = hardToMove ? hard : medium;
            SplittableRandom random = hardToMove ? hardRandom : mediumRandom;
            Move move = strategy.chooseMove(game, random);
            game.makeMove(move);
        }
        return game.status();
    }

    private static boolean isWinFor(GameStatus result, Side hardSide) {
        return (hardSide == Side.BLACK && result == GameStatus.BLACK_WON)
                || (hardSide == Side.WHITE && result == GameStatus.WHITE_WON);
    }
}
