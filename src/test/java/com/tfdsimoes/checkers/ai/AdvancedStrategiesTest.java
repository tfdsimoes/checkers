package com.tfdsimoes.checkers.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.SplittableRandom;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

class AdvancedStrategiesTest {
    @Test
    void mediumAndHardPoolsContainThePlannedAlgorithmsAndBudgets() {
        AlgorithmRegistry registry = new AlgorithmRegistry();

        assertTrue(registry.strategies(Difficulty.MEDIUM).stream()
                .anyMatch(strategy -> strategy instanceof MctsStrategy));
        assertTrue(registry.strategies(Difficulty.HARD).stream()
                .anyMatch(strategy -> strategy instanceof IterativeDeepeningStrategy));
        assertTrue(registry.strategies(Difficulty.HARD).stream()
                .anyMatch(strategy -> strategy instanceof QuiescenceStrategy));
        assertEquals("MCTS 1000 iterations",
                registry.strategies(Difficulty.MEDIUM).get(2).name());
        assertEquals("MCTS 5000 iterations",
                registry.strategies(Difficulty.HARD).get(2).name());
    }

    @Test
    void advancedStrategiesChooseLegalMovesFromKingAndCapturePositions() {
        AlgorithmRegistry registry = new AlgorithmRegistry();
        GameState kings = kingsPosition();
        GameState forcedCapture = forcedCapturePosition();

        for (Difficulty difficulty : new Difficulty[]{Difficulty.MEDIUM, Difficulty.HARD}) {
            for (AiStrategy strategy : registry.strategies(difficulty)) {
                assertLegalMove(strategy, kings, 300 + difficulty.ordinal());
                assertLegalMove(strategy, forcedCapture, 400 + difficulty.ordinal());
            }
        }
    }

    @Test
    void mctsIsReproducibleWithTheSameSeedAndIterationCount() {
        GameState game = kingsPosition();
        MctsStrategy strategy = new MctsStrategy(100);

        Move first = strategy.chooseMove(game, new SplittableRandom(1234));
        Move second = strategy.chooseMove(game, new SplittableRandom(1234));

        assertEquals(first, second);
    }

    @Test
    void mctsTreatsTheSideWithoutMovesAsTheLoser() {
        assertEquals(0.0, MctsStrategy.noMovesOutcome(Side.BLACK, Side.BLACK));
        assertEquals(1.0, MctsStrategy.noMovesOutcome(Side.BLACK, Side.WHITE));
    }

    @Test
    void aiSearchStopsWhenItsWorkerIsInterrupted() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class,
                    () -> new MctsStrategy(5_000).chooseMove(kingsPosition(), new SplittableRandom(7)));
        } finally {
            Thread.interrupted();
        }
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
