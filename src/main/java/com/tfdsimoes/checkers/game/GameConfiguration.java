package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.model.Side;
import java.util.Objects;

public record GameConfiguration(GameMode mode, Difficulty difficulty, Side firstPlayerSide, long seed) {
    public GameConfiguration {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(firstPlayerSide, "firstPlayerSide");
    }

    public GameConfiguration nextGame() {
        return new GameConfiguration(mode, difficulty, firstPlayerSide.opposite(), seed);
    }
}
