package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class AlphaBetaStrategy implements AiStrategy {
    private static final int NEGATIVE_INFINITY = Integer.MIN_VALUE + 1;
    private static final int POSITIVE_INFINITY = Integer.MAX_VALUE;

    private final int depth;
    private final EvaluationFunction evaluation = new EvaluationFunction();

    public AlphaBetaStrategy(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("Search depth must be positive");
        }
        this.depth = depth;
    }

    @Override
    public String name() {
        return "Alpha-beta depth " + depth + " with move ordering";
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> moves = ordered(game.legalMoves());
        if (moves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }
        Side perspective = game.currentPlayer();
        int bestScore = NEGATIVE_INFINITY;
        List<Move> bestMoves = new ArrayList<>();
        for (Move move : moves) {
            SearchCancellation.checkInterrupted();
            GameState next = game.searchCopy();
            next.makeMove(move);
            int score = search(next, depth - 1, perspective, NEGATIVE_INFINITY, POSITIVE_INFINITY, 1);
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
        return search(game, depth, perspective, NEGATIVE_INFINITY, POSITIVE_INFINITY, 0);
    }

    private int search(
            GameState game, int remainingDepth, Side perspective, int alpha, int beta, int ply) {
        SearchCancellation.checkInterrupted();
        if (remainingDepth == 0 || game.status() != com.tfdsimoes.checkers.game.GameStatus.IN_PROGRESS) {
            return evaluation.evaluate(game, perspective, ply);
        }
        List<Move> moves = ordered(game.legalMoves());
        if (moves.isEmpty()) {
            return evaluation.evaluate(game, perspective, ply);
        }
        boolean maximizing = game.currentPlayer() == perspective;
        int bestScore = maximizing ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
        for (Move move : moves) {
            GameState next = game.searchCopy();
            next.makeMove(move);
            int score = search(next, remainingDepth - 1, perspective, alpha, beta, ply + 1);
            if (maximizing) {
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
            } else {
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);
            }
            if (beta <= alpha) {
                break;
            }
        }
        return bestScore;
    }

    private static List<Move> ordered(List<Move> moves) {
        return moves.stream()
                .sorted(Comparator.comparingInt((Move move) -> move.captures().size()).reversed()
                        .thenComparingInt(move -> move.to().row()))
                .toList();
    }
}
