package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.ai.AiStrategy;
import com.tfdsimoes.checkers.model.Move;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class AiController implements PlayerController {
    private final AiStrategy strategy;
    private final RandomGenerator random;

    public AiController(AiStrategy strategy, RandomGenerator random) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public boolean isHuman() {
        return false;
    }

    @Override
    public String displayName() {
        return strategy.name();
    }

    @Override
    public Move chooseMove(GameState game) {
        return strategy.chooseMove(game, random);
    }
}
