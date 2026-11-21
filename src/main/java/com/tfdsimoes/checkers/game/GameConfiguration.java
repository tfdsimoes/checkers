package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.model.Side;
import java.util.Objects;
import java.util.SplittableRandom;

public record GameConfiguration(
        GameMode mode, Difficulty difficulty, Side firstPlayerSide, long seed, boolean randomSeed) {
    public GameConfiguration(GameMode mode, Difficulty difficulty, Side firstPlayerSide, long seed) {
        this(mode, difficulty, firstPlayerSide, seed, false);
    }

    public GameConfiguration {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(firstPlayerSide, "firstPlayerSide");
    }

    public static GameConfiguration withRandomSeed(
            GameMode mode, Difficulty difficulty, Side firstPlayerSide, long seed) {
        return new GameConfiguration(mode, difficulty, firstPlayerSide, seed, true);
    }

    public GameConfiguration nextGame() {
        long nextSeed = randomSeed ? differentRandomSeed(seed) : seed + 1;
        return new GameConfiguration(mode, difficulty, firstPlayerSide.opposite(), nextSeed, randomSeed);
    }

    private static long differentRandomSeed(long previousSeed) {
        SplittableRandom random = new SplittableRandom();
        long nextSeed;
        do {
            nextSeed = random.nextLong();
        } while (nextSeed == previousSeed);
        return nextSeed;
    }
}
