package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class GreedyCaptureStrategy implements AiStrategy {
    private final EvaluationFunction evaluation = new EvaluationFunction();

    @Override
    public String name() {
        return "Greedy one-ply capture";
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> moves = game.legalMoves();
        if (moves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }
        int bestScore = Integer.MIN_VALUE;
        List<Move> bestMoves = new ArrayList<>();
        for (Move move : moves) {
            SearchCancellation.checkInterrupted();
            GameState next = game.copy();
            next.makeMove(move);
            int score = evaluation.evaluate(next, game.currentPlayer());
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
            }
            if (score == bestScore) {
                bestMoves.add(move);
            }
        }
        return bestMoves.get(random.nextInt(bestMoves.size()));
    }
}
