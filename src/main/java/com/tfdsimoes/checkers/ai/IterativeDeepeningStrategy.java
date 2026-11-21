package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class IterativeDeepeningStrategy implements AiStrategy {
    private static final int NEGATIVE_INFINITY = Integer.MIN_VALUE + 1;
    private static final int POSITIVE_INFINITY = Integer.MAX_VALUE;
    private static final int MAX_TABLE_ENTRIES = 200_000;

    private final int maximumDepth;
    private final EvaluationFunction evaluation = new EvaluationFunction();

    public IterativeDeepeningStrategy(int maximumDepth) {
        if (maximumDepth < 1) {
            throw new IllegalArgumentException("Maximum depth must be positive");
        }
        this.maximumDepth = maximumDepth;
    }

    @Override
    public String name() {
        return "Iterative deepening alpha-beta with transposition table (depth " + maximumDepth + ")";
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> legalMoves = ordered(game.legalMoves(), null, null, 0);
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }

        SearchContext context = new SearchContext(maximumDepth);
        Move bestMove = legalMoves.get(0);
        for (int depth = 1; depth <= maximumDepth; depth++) {
            SearchCancellation.checkInterrupted();
            int bestScore = NEGATIVE_INFINITY;
            List<Move> bestMoves = new ArrayList<>();
            TableEntry rootEntry = context.table.get(ZobristHasher.hash(game));
            Move tableMove = rootEntry == null ? null : rootEntry.bestMove;
            List<Move> moves = ordered(game.legalMoves(), tableMove, context.killers, 0);
            for (Move move : moves) {
                SearchCancellation.checkInterrupted();
                GameState next = game.searchCopy();
                next.makeMove(move);
                int score = -search(next, depth - 1, -POSITIVE_INFINITY, POSITIVE_INFINITY, 1, context);
                if (score > bestScore) {
                    bestScore = score;
                    bestMoves.clear();
                }
                if (score == bestScore) {
                    bestMoves.add(move);
                }
            }
            bestMove = bestMoves.get(random.nextInt(bestMoves.size()));
        }
        return bestMove;
    }

    private int search(
            GameState game,
            int depth,
            int alpha,
            int beta,
            int ply,
            SearchContext context) {
        SearchCancellation.checkInterrupted();
        if (game.status() != GameStatus.IN_PROGRESS) {
            return evaluation.evaluate(game, game.currentPlayer());
        }
        if (depth == 0) {
            return evaluation.evaluate(game, game.currentPlayer());
        }

        int originalAlpha = alpha;
        int originalBeta = beta;
        long hash = ZobristHasher.hash(game);
        TableEntry entry = context.table.get(hash);
        Move tableMove = null;
        if (entry != null) {
            tableMove = entry.bestMove;
            if (entry.depth >= depth) {
                if (entry.bound == Bound.EXACT) {
                    return entry.score;
                }
                if (entry.bound == Bound.LOWER) {
                    alpha = Math.max(alpha, entry.score);
                } else {
                    beta = Math.min(beta, entry.score);
                }
                if (beta <= alpha) {
                    return entry.score;
                }
            }
        }

        List<Move> moves = ordered(game.legalMoves(), tableMove, context.killers, ply);
        if (moves.isEmpty()) {
            return evaluation.evaluate(game, game.currentPlayer());
        }
        int bestScore = NEGATIVE_INFINITY;
        Move bestMove = null;
        for (Move move : moves) {
            GameState next = game.searchCopy();
            next.makeMove(move);
            int score = -search(next, depth - 1, -beta, -alpha, ply + 1, context);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) {
                rememberKiller(move, ply, context.killers);
                break;
            }
        }

        if (context.table.size() < MAX_TABLE_ENTRIES || context.table.containsKey(hash)) {
            Bound bound = bestScore <= originalAlpha
                    ? Bound.UPPER
                    : bestScore >= originalBeta ? Bound.LOWER : Bound.EXACT;
            TableEntry previous = context.table.get(hash);
            if (previous == null || previous.depth <= depth) {
                context.table.put(hash, new TableEntry(depth, bestScore, bound, bestMove));
            }
        }
        return bestScore;
    }

    private static List<Move> ordered(List<Move> moves, Move tableMove, Move[][] killers, int ply) {
        return moves.stream()
                .sorted(Comparator.comparingInt((Move move) -> moveOrderScore(move, tableMove, killers, ply))
                        .reversed()
                        .thenComparingInt(move -> move.to().row()))
                .toList();
    }

    private static int moveOrderScore(Move move, Move tableMove, Move[][] killers, int ply) {
        int score = move.captures().size() * 1_000;
        if (isKiller(move, killers, ply)) {
            score += 100;
        }
        if (move.equals(tableMove)) {
            score += 100_000;
        }
        return score;
    }

    private static boolean isKiller(Move move, Move[][] killers, int ply) {
        return killers != null && ply < killers.length
                && (move.equals(killers[ply][0]) || move.equals(killers[ply][1]));
    }

    private static void rememberKiller(Move move, int ply, Move[][] killers) {
        if (move.isCapture() || ply >= killers.length || move.equals(killers[ply][0])) {
            return;
        }
        killers[ply][1] = killers[ply][0];
        killers[ply][0] = move;
    }

    private enum Bound {
        EXACT,
        LOWER,
        UPPER
    }

    private record TableEntry(int depth, int score, Bound bound, Move bestMove) {
    }

    private static final class SearchContext {
        private final Map<Long, TableEntry> table = new HashMap<>();
        private final Move[][] killers;

        private SearchContext(int maximumDepth) {
            killers = new Move[maximumDepth + 1][2];
        }
    }
}
