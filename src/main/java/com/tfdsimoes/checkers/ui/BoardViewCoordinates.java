package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.Objects;

final class BoardViewCoordinates {
    private BoardViewCoordinates() {
    }

    static boolean shouldFlipForWhitePlayer(GameMode mode, Side humanSide) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(humanSide, "humanSide");
        return mode == GameMode.PLAYER_VS_MACHINE && humanSide == Side.WHITE;
    }

    static Position toBoardPosition(int viewRow, int viewColumn, boolean flipped) {
        if (viewRow < 0 || viewRow >= Board.SIZE || viewColumn < 0 || viewColumn >= Board.SIZE) {
            throw new IllegalArgumentException("View coordinates must be within the board");
        }
        return flipped
                ? new Position(Board.SIZE - 1 - viewRow, Board.SIZE - 1 - viewColumn)
                : new Position(viewRow, viewColumn);
    }

    static Position toViewPosition(Position position, boolean flipped) {
        Objects.requireNonNull(position, "position");
        if (position.row() < 0 || position.row() >= Board.SIZE
                || position.column() < 0 || position.column() >= Board.SIZE) {
            throw new IllegalArgumentException("Position must be within the board");
        }
        return flipped
                ? new Position(Board.SIZE - 1 - position.row(), Board.SIZE - 1 - position.column())
                : position;
    }
}
