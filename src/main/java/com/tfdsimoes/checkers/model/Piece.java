package com.tfdsimoes.checkers.model;

import java.util.Objects;

public record Piece(Side side, boolean king) {
    public Piece {
        Objects.requireNonNull(side, "side");
    }

    public Piece promote() {
        return king ? this : new Piece(side, true);
    }
}
