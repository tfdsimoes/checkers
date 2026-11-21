package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class QuiescenceStrategy implements AiStrategy {
    private static final int NEGATIVE_INFINITY = Integer.MIN_VALUE + 1;
    private static final int POSITIVE_INFINITY = Integer.MAX_VALUE;

    private final int depth;
    private final int maximumQuiescenceNodes;
    private final EvaluationFunction evaluation = new EvaluationFunction();

    public QuiescenceStrategy(int depth, int maximumQuiescenceNodes) {
        if (depth < 1 || maximumQuiescenceNodes < 1) {
            throw new IllegalArgumentException("Search depth and quiescence node limit must be positive");
        }
        this.depth = depth;
        this.maximumQuiescenceNodes = maximumQuiescenceNodes;
    }

    @Override
    public String name() {
        return "Alpha-beta with quiescence (depth " + depth + ", cap "
                + maximumQuiescenceNodes + " nodes)";
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> moves = ordered(game.legalMoves());
        if (moves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }
        Side perspective = game.currentPlayer();
        int[] quiescenceNodes = {0};
        int bestScore = NEGATIVE_INFINITY;
        List<Move> bestMoves = new ArrayList<>();
        for (Move move : moves) {
            SearchCancellation.checkInterrupted();
            GameState next = game.searchCopy();
            next.makeGeneratedMove(move);
            int score = -search(next, depth - 1, NEGATIVE_INFINITY, POSITIVE_INFINITY, 1, quiescenceNodes);
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

    private int search(GameState game, int depth, int alpha, int beta, int ply, int[] quiescenceNodes) {
        SearchCancellation.checkInterrupted();
        if (game.status() != GameStatus.IN_PROGRESS) {
            return evaluation.evaluate(game, game.currentPlayer(), ply);
        }
        if (depth == 0) {
            return quiescence(game, alpha, beta, ply, quiescenceNodes);
        }
        List<Move> moves = ordered(game.legalMoves());
        if (moves.isEmpty()) {
            return evaluation.evaluate(game, game.currentPlayer(), ply);
        }
        int bestScore = NEGATIVE_INFINITY;
        for (Move move : moves) {
            GameState next = game.searchCopy();
            next.makeGeneratedMove(move);
            int score = -search(next, depth - 1, -beta, -alpha, ply + 1, quiescenceNodes);
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, bestScore);
            if (alpha >= beta) {
                break;
            }
        }
        return bestScore;
    }

    private int quiescence(GameState game, int alpha, int beta, int ply, int[] nodes) {
        SearchCancellation.checkInterrupted();
        if (game.status() != GameStatus.IN_PROGRESS) {
            return evaluation.evaluate(game, game.currentPlayer(), ply);
        }
        if (nodes[0] >= maximumQuiescenceNodes) {
            return evaluation.evaluate(game, game.currentPlayer(), ply);
        }
        List<Move> moves = game.legalMoves();
        if (moves.isEmpty() || moves.stream().noneMatch(Move::isCapture)) {
            return evaluation.evaluate(game, game.currentPlayer(), ply);
        }
        int bestScore = NEGATIVE_INFINITY;
        for (Move move : ordered(moves)) {
            if (!move.isCapture()) {
                continue;
            }
            nodes[0]++;
            GameState next = game.searchCopy();
            next.makeGeneratedMove(move);
            int score = -quiescence(next, -beta, -alpha, ply + 1, nodes);
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, bestScore);
            if (alpha >= beta || nodes[0] >= maximumQuiescenceNodes) {
                break;
            }
        }
        return bestScore == NEGATIVE_INFINITY
                ? evaluation.evaluate(game, game.currentPlayer(), ply)
                : bestScore;
    }

    private static List<Move> ordered(List<Move> moves) {
        return moves.stream()
                .sorted(Comparator.comparingInt((Move move) -> move.captures().size()).reversed())
                .toList();
    }
}
