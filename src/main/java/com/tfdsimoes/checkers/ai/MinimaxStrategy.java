package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class MinimaxStrategy implements AiStrategy {
    private static final int NEGATIVE_INFINITY = Integer.MIN_VALUE + 1;
    private static final int POSITIVE_INFINITY = Integer.MAX_VALUE;

    private final int depth;
    private final EvaluationFunction evaluation = new EvaluationFunction();

    public MinimaxStrategy(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("Search depth must be positive");
        }
        this.depth = depth;
    }

    @Override
    public String name() {
        return "Minimax depth " + depth;
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> moves = game.legalMoves();
        if (moves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }
        Side perspective = game.currentPlayer();
        int bestScore = NEGATIVE_INFINITY;
        List<Move> bestMoves = new ArrayList<>();
        for (Move move : moves) {
            GameState next = game.copy();
            next.makeMove(move);
            int score = search(next, depth - 1, perspective);
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

    int scorePosition(GameState game, Side perspective) {
        return search(game, depth, perspective);
    }

    private int search(GameState game, int remainingDepth, Side perspective) {
        if (remainingDepth == 0 || game.status() != com.tfdsimoes.checkers.game.GameStatus.IN_PROGRESS) {
            return evaluation.evaluate(game, perspective);
        }
        List<Move> moves = game.legalMoves();
        if (moves.isEmpty()) {
            return evaluation.evaluate(game, perspective);
        }
        boolean maximizing = game.currentPlayer() == perspective;
        int bestScore = maximizing ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
        for (Move move : moves) {
            GameState next = game.copy();
            next.makeMove(move);
            int score = search(next, remainingDepth - 1, perspective);
            bestScore = maximizing ? Math.max(bestScore, score) : Math.min(bestScore, score);
        }
        return bestScore;
    }
}
