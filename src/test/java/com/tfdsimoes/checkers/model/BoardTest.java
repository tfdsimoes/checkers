package com.tfdsimoes.checkers.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardTest {
    @Test
    void startingPositionHasTwelvePiecesPerSide() {
        Board board = Board.startingPosition();

        assertEquals(12, board.pieceCount(Side.BLACK));
        assertEquals(12, board.pieceCount(Side.WHITE));
        assertEquals(new Piece(Side.BLACK, false), board.pieceAt(Position.fromSquareNumber(1)));
        assertEquals(new Piece(Side.WHITE, false), board.pieceAt(Position.fromSquareNumber(32)));
    }

    @Test
    void squareNumberingRoundTripsAllPlayableSquares() {
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            assertEquals(square, Position.fromSquareNumber(square).squareNumber());
        }
        assertTrue(Position.fromSquareNumber(1).row() < Position.fromSquareNumber(32).row());
    }

    @Test
    void boardCopyDoesNotShareMutableGrid() {
        Board board = Board.startingPosition();
        Board copy = board.copy();
        copy.setPiece(Position.fromSquareNumber(1), null);

        assertNotSame(board, copy);
        assertEquals(new Piece(Side.BLACK, false), board.pieceAt(Position.fromSquareNumber(1)));
    }

    @Test
    void rejectsUnplayablePositionsAndInvalidSquareNumbers() {
        assertThrows(IllegalStateException.class, () -> new Position(0, 0).squareNumber());
        assertThrows(IllegalArgumentException.class, () -> Position.fromSquareNumber(33));
        assertThrows(IllegalArgumentException.class,
                () -> new Board().setPiece(new Position(0, 0), new Piece(Side.BLACK, false)));
    }
}
