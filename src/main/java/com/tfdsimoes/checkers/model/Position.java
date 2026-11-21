package com.tfdsimoes.checkers.model;

public record Position(int row, int column) {
    public Position {
        if (row < 0 || row >= Board.SIZE || column < 0 || column >= Board.SIZE) {
            throw new IllegalArgumentException("Position is outside the board");
        }
    }

    public boolean isPlayable() {
        return (row + column) % 2 == 1;
    }

    public int squareNumber() {
        if (!isPlayable()) {
            throw new IllegalStateException("Position is not a playable square");
        }
        return row * 4 + column / 2 + 1;
    }

    public static Position fromSquareNumber(int squareNumber) {
        if (squareNumber < 1 || squareNumber > Board.PLAYABLE_SQUARES) {
            throw new IllegalArgumentException("Square number must be between 1 and 32");
        }
        int zeroBased = squareNumber - 1;
        int row = zeroBased / 4;
        int column = (zeroBased % 4) * 2 + (row % 2 == 0 ? 1 : 0);
        return new Position(row, column);
    }
}
