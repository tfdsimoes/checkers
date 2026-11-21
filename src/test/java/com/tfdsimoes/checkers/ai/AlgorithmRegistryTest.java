package com.tfdsimoes.checkers.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameSeed;
import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class AlgorithmRegistryTest {
    private final AlgorithmRegistry registry = new AlgorithmRegistry();

    @Test
    void everyDifficultyHasAtLeastThreeStrategiesAndSelectionReturnsPoolMember() {
        for (Difficulty difficulty : Difficulty.values()) {
            List<AiStrategy> pool = registry.strategies(difficulty);
            assertTrue(pool.size() >= 3);

            AiStrategy selected = registry.select(difficulty, new GameSeed(42).random());
            assertTrue(pool.contains(selected));
            assertEquals(selected.name(),
                    registry.select(difficulty, new GameSeed(42).random()).name());
        }
    }

    @Test
    void allStrategiesReturnLegalMovesFromKingAndForcedCapturePositions() {
        GameState kings = kingsPosition();
        GameState forcedCapture = forcedCapturePosition();

        for (Difficulty difficulty : Difficulty.values()) {
            for (AiStrategy strategy : registry.strategies(difficulty)) {
                assertLegalMove(strategy, kings, 100 + difficulty.ordinal());
                assertLegalMove(strategy, forcedCapture, 200 + difficulty.ordinal());
            }
        }
    }

    @Test
    void easyStrategiesReturnLegalMovesFromStartingPosition() {
        GameState startingPosition = new GameState();
        for (AiStrategy strategy : registry.strategies(Difficulty.EASY)) {
            assertLegalMove(strategy, startingPosition, 7);
        }
    }

    @Test
    void alphaBetaMatchesMinimaxAtTheSameDepth() {
        GameState game = new GameState();

        assertEquals(new MinimaxStrategy(3).scorePosition(game, Side.BLACK),
                new AlphaBetaStrategy(3).scorePosition(game, Side.BLACK));
    }

    private static void assertLegalMove(AiStrategy strategy, GameState game, long seed) {
        Move move = strategy.chooseMove(game, new SplittableRandom(seed));
        assertTrue(game.legalMoves().contains(move), strategy.name() + " returned an illegal move");
    }

    private static GameState kingsPosition() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, true));
        board.setPiece(square(24), new Piece(Side.WHITE, true));
        return new GameState(board, Side.BLACK);
    }

    private static GameState forcedCapturePosition() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, false));
        board.setPiece(square(14), new Piece(Side.WHITE, false));
        board.setPiece(square(22), new Piece(Side.WHITE, false));
        return new GameState(board, Side.BLACK);
    }

    private static Position square(int number) {
        return Position.fromSquareNumber(number);
    }
}
