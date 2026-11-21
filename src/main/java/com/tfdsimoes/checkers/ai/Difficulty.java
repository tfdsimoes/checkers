package com.tfdsimoes.checkers.ai;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    @Override
    public String toString() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
}
