package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Move;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class RandomStrategy implements AiStrategy {
    @Override
    public String name() {
        return "Random";
    }

    @Override
    public Move chooseMove(GameState game, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<Move> moves = game.legalMoves();
        if (moves.isEmpty()) {
            throw new IllegalStateException("No legal move is available");
        }
        return moves.get(random.nextInt(moves.size()));
    }
}
