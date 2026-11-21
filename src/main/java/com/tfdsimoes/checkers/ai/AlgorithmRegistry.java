package com.tfdsimoes.checkers.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class AlgorithmRegistry {
    private final Map<Difficulty, List<AiStrategy>> strategies;

    public AlgorithmRegistry() {
        strategies = Map.of(
                Difficulty.EASY, List.of(
                        new RandomStrategy(),
                        new GreedyCaptureStrategy(),
                        new MinimaxStrategy(2)),
                Difficulty.MEDIUM, List.of(
                        new AlphaBetaStrategy(4),
                        new AlphaBetaStrategy(6),
                        new MinimaxStrategy(6)),
                Difficulty.HARD, List.of(
                        new AlphaBetaStrategy(6),
                        new AlphaBetaStrategy(7),
                        new AlphaBetaStrategy(8)));
    }

    public List<AiStrategy> strategies(Difficulty difficulty) {
        return strategies.get(Objects.requireNonNull(difficulty, "difficulty"));
    }

    public AiStrategy select(Difficulty difficulty, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        List<AiStrategy> pool = strategies(difficulty);
        return pool.get(random.nextInt(pool.size()));
    }
}
