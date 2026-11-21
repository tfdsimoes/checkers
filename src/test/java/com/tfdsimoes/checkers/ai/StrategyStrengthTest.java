package com.tfdsimoes.checkers.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class StrategyStrengthTest {
    private static final int GAMES_PER_STRATEGY = 20;
    private static final int MINIMUM_WINS = 16;
    private static final int MAXIMUM_LOSSES = 2;

    @Test
    void everyHardStrategyMeetsTheSeededWinRateTargetAgainstTheEasyPool() {
        AlgorithmRegistry registry = new AlgorithmRegistry();

        for (int strategyIndex = 0;
                strategyIndex < registry.strategies(Difficulty.HARD).size();
                strategyIndex++) {
            AiStrategy hard = registry.strategies(Difficulty.HARD).get(strategyIndex);
            int wins = 0;
            int losses = 0;
            for (int gameIndex = 0; gameIndex < GAMES_PER_STRATEGY; gameIndex++) {
                long seed = 0x5EEDL + strategyIndex * 1_000L + gameIndex;
                AiStrategy easy = registry.select(
                        Difficulty.EASY, new SplittableRandom(seed ^ 0xA11CE));
                Side hardSide = gameIndex % 2 == 0 ? Side.BLACK : Side.WHITE;
                GameStatus result = playGame(
                        hard,
                        easy,
                        hardSide,
                        new SplittableRandom(seed ^ 0xC0FFEE),
                        new SplittableRandom(seed ^ 0xBADC0DE));
                if (isWinFor(result, hardSide)) {
                    wins++;
                } else if (result != GameStatus.DRAW) {
                    losses++;
                }
            }
            assertTrue(wins >= MINIMUM_WINS,
                    hard.name() + " won " + wins + " of " + GAMES_PER_STRATEGY + " games");
            assertTrue(losses <= MAXIMUM_LOSSES,
                    hard.name() + " lost " + losses + " of " + GAMES_PER_STRATEGY + " games");
        }
    }

    private static GameStatus playGame(
            AiStrategy hard,
            AiStrategy easy,
            Side hardSide,
            SplittableRandom hardRandom,
            SplittableRandom easyRandom) {
        GameState game = new GameState();
        while (game.status() == GameStatus.IN_PROGRESS) {
            boolean hardToMove = game.currentPlayer() == hardSide;
            AiStrategy strategy = hardToMove ? hard : easy;
            SplittableRandom random = hardToMove ? hardRandom : easyRandom;
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
