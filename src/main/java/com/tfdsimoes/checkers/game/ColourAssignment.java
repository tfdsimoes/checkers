package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.model.Side;
import java.util.Objects;

public record ColourAssignment(Side firstPlayerSide) {
    public ColourAssignment {
        Objects.requireNonNull(firstPlayerSide, "firstPlayerSide");
    }

    public static ColourAssignment startingWithBlack() {
        return new ColourAssignment(Side.BLACK);
    }

    public Side secondPlayerSide() {
        return firstPlayerSide.opposite();
    }

    public ColourAssignment swappedForNextGame() {
        return new ColourAssignment(secondPlayerSide());
    }
}
