package com.tfdsimoes.checkers.game;

import java.util.SplittableRandom;

public record GameSeed(long value) {
    public SplittableRandom random() {
        return new SplittableRandom(value);
    }
}
