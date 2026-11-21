package com.tfdsimoes.checkers.model;

import java.util.Objects;

public final class Board {
    public static final int SIZE = 8;
    public static final int PLAYABLE_SQUARES = 32;
    private final Piece[][] squares;

    public Board() {
        squares = new Piece[SIZE][SIZE];
    }

    private Board(Piece[][] squares) {
        this.squares = squares;
    }

    public static Board startingPosition() {
        Board board = new Board();
        for (int square = 1; square <= PLAYABLE_SQUARES; square++) {
            Position position = Position.fromSquareNumber(square);
            if (position.row() < 3) {
                board.setPiece(position, new Piece(Side.BLACK, false));
            } else if (position.row() >= 5) {
                board.setPiece(position, new Piece(Side.WHITE, false));
            }
        }
        return board;
    }

    public Piece pieceAt(Position position) {
        Objects.requireNonNull(position, "position");
        return squares[position.row()][position.column()];
    }

    public void setPiece(Position position, Piece piece) {
        Objects.requireNonNull(position, "position");
        if (!position.isPlayable()) {
            throw new IllegalArgumentException("Pieces can only occupy playable squares");
        }
        squares[position.row()][position.column()] = piece;
    }

    public Board copy() {
        Piece[][] copy = new Piece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(squares[row], 0, copy[row], 0, SIZE);
        }
        return new Board(copy);
    }

    public int pieceCount(Side side) {
        Objects.requireNonNull(side, "side");
        int count = 0;
        for (Piece[] row : squares) {
            for (Piece piece : row) {
                if (piece != null && piece.side() == side) {
                    count++;
                }
            }
        }
        return count;
    }
}
