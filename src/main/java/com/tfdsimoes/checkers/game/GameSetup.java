package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.ai.AlgorithmRegistry;
import com.tfdsimoes.checkers.ai.AiStrategy;
import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.model.Side;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;

public final class GameSetup {
    private final GameConfiguration configuration;
    private final GameState game;
    private final Map<Side, PlayerController> controllers;

    private GameSetup(
            GameConfiguration configuration,
            GameState game,
            Map<Side, PlayerController> controllers) {
        this.configuration = configuration;
        this.game = game;
        this.controllers = Map.copyOf(controllers);
    }

    public static GameSetup create(GameConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        SplittableRandom random = new GameSeed(configuration.seed()).random();
        AlgorithmRegistry registry = new AlgorithmRegistry();
        PlayerController firstPlayer;
        PlayerController secondPlayer;
        switch (configuration.mode()) {
            case PLAYER_VS_PLAYER -> {
                firstPlayer = new HumanController("Player 1");
                secondPlayer = new HumanController("Player 2");
            }
            case PLAYER_VS_MACHINE -> {
                firstPlayer = new HumanController("Player");
                secondPlayer = new AiController(
                        registry.select(configuration.difficulty(), random), random);
            }
            case MACHINE_VS_MACHINE -> {
                firstPlayer = new AiController(registry.select(Difficulty.HARD, random), random);
                secondPlayer = new AiController(registry.select(Difficulty.HARD, random), random);
            }
            default -> throw new IllegalStateException("Unsupported game mode: " + configuration.mode());
        }

        PlayerController black = configuration.firstPlayerSide() == Side.BLACK ? firstPlayer : secondPlayer;
        PlayerController white = configuration.firstPlayerSide() == Side.WHITE ? firstPlayer : secondPlayer;
        return new GameSetup(configuration, new GameState(), Map.of(Side.BLACK, black, Side.WHITE, white));
    }

    public GameConfiguration configuration() {
        return configuration;
    }

    public GameState game() {
        return game;
    }

    public PlayerController controllerFor(Side side) {
        return controllers.get(side);
    }

    public String algorithmNames() {
        return controllers.entrySet().stream()
                .filter(entry -> !entry.getValue().isHuman())
                .map(entry -> entry.getKey() + ": " + entry.getValue().displayName())
                .sorted()
                .collect(java.util.stream.Collectors.joining(" | "));
    }
}
