package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class MctsStrategy implements AiStrategy {
    private static final double EXPLORATION = Math.sqrt(2.0);

    private final int iterations;

    public MctsStrategy(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("MCTS iterations must be positive");
        }
        this.iterations = iterations;
    }

    @Override
    public String name() {
        return "MCTS " + iterations + " iterations";
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> legalMoves = game.legalMoves();
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }
        Side rootSide = game.currentPlayer();
        Node root = new Node(game.searchCopy(), null, null);

        for (int iteration = 0; iteration < iterations; iteration++) {
            SearchCancellation.checkInterrupted();
            Node node = root;
            while (!node.isTerminal() && node.isFullyExpanded() && !node.children.isEmpty()) {
                node = selectChild(node, rootSide, random);
            }
            if (!node.isTerminal() && !node.unexpandedMoves.isEmpty()) {
                int index = random.nextInt(node.unexpandedMoves.size());
                Move move = node.unexpandedMoves.remove(index);
                GameState childState = node.state.searchCopy();
                childState.makeMove(move);
                Node child = new Node(childState, node, move);
                node.children.add(child);
                node = child;
            }

            double outcome = rollout(node.state.searchCopy(), rootSide, random);
            for (Node visited = node; visited != null; visited = visited.parent) {
                visited.visits++;
                visited.value += outcome;
            }
        }

        int mostVisits = root.children.stream().mapToInt(child -> child.visits).max().orElse(0);
        List<Node> best = root.children.stream()
                .filter(child -> child.visits == mostVisits)
                .toList();
        return best.get(random.nextInt(best.size())).move;
    }

    private static Node selectChild(Node parent, Side rootSide, RandomGenerator random) {
        boolean maximizing = parent.state.currentPlayer() == rootSide;
        double logParentVisits = Math.log(parent.visits);
        double bestScore = Double.NEGATIVE_INFINITY;
        List<Node> best = new ArrayList<>();
        for (Node child : parent.children) {
            double mean = child.value / child.visits;
            double exploitation = maximizing ? mean : -mean;
            double score = exploitation + EXPLORATION * Math.sqrt(logParentVisits / child.visits);
            if (score > bestScore) {
                bestScore = score;
                best.clear();
            }
            if (Double.compare(score, bestScore) == 0) {
                best.add(child);
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    private static double rollout(GameState game, Side rootSide, RandomGenerator random) {
        while (game.status() == GameStatus.IN_PROGRESS) {
            SearchCancellation.checkInterrupted();
            List<Move> moves = game.legalMoves();
            if (moves.isEmpty()) {
                break;
            }
            game.makeMove(moves.get(random.nextInt(moves.size())));
        }
        if (game.status() == GameStatus.DRAW) {
            return 0.5;
        }
        Side winner = game.status() == GameStatus.BLACK_WON ? Side.BLACK : Side.WHITE;
        return winner == rootSide ? 1.0 : 0.0;
    }

    private static final class Node {
        private final GameState state;
        private final Node parent;
        private final Move move;
        private final List<Node> children = new ArrayList<>();
        private final List<Move> unexpandedMoves;
        private int visits;
        private double value;

        private Node(GameState state, Node parent, Move move) {
            this.state = state;
            this.parent = parent;
            this.move = move;
            unexpandedMoves = new ArrayList<>(state.legalMoves());
        }

        private boolean isFullyExpanded() {
            return unexpandedMoves.isEmpty();
        }

        private boolean isTerminal() {
            return state.status() != GameStatus.IN_PROGRESS;
        }
    }
}
