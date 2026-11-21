package com.tfdsimoes.checkers.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class SearchBenchmark {
    private static final int POSITION_COUNT = 20;
    private static final int POSITION_PLIES = 16;
    private static final long POSITION_SEED = 0x5EEDL;

    @Test
    void reportsHardSearchNodesPerSecondOnFixedSeededPositions() {
        List<GameState> positions = positions();
        IterativeDeepeningStrategy strategy = new IterativeDeepeningStrategy(8);
        long totalNodes = 0;
        long startedAt = System.nanoTime();
        for (int index = 0; index < positions.size(); index++) {
            strategy.chooseMove(positions.get(index), new SplittableRandom(POSITION_SEED + index));
            totalNodes += strategy.lastNodesVisited();
        }
        long elapsedNanos = System.nanoTime() - startedAt;
        double nodesPerSecond = totalNodes * 1_000_000_000.0 / elapsedNanos;

        assertTrue(totalNodes > 0);
        assertTrue(elapsedNanos > 0);
        System.out.printf(Locale.ROOT,
                "SearchBenchmark: positions=%d nodes=%d elapsedMs=%.1f nodesPerSecond=%.0f%n",
                positions.size(), totalNodes, elapsedNanos / 1_000_000.0, nodesPerSecond);
    }

    private static List<GameState> positions() {
        List<GameState> positions = new ArrayList<>(POSITION_COUNT);
        for (int index = 0; index < POSITION_COUNT; index++) {
            GameState game = new GameState();
            SplittableRandom random = new SplittableRandom(POSITION_SEED + index);
            for (int ply = 0; ply < POSITION_PLIES && game.status() == GameStatus.IN_PROGRESS; ply++) {
                List<Move> moves = game.legalMoves();
                if (moves.isEmpty()) {
                    break;
                }
                game.makeMove(moves.get(random.nextInt(moves.size())));
            }
            positions.add(game);
        }
        return List.copyOf(positions);
    }
}
