package com.tfdsimoes.checkers.model;

public enum Side {
    BLACK,
    WHITE;

    public Side opposite() {
        return this == BLACK ? WHITE : BLACK;
    }

    public int forwardDirection() {
        return this == BLACK ? 1 : -1;
    }
}
