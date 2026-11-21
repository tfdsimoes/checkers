package com.tfdsimoes.checkers.rules;

import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.Objects;

public final class MoveApplier {
    private MoveApplier() {
    }

    public static void apply(Board board, Move move) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(move, "move");
        Piece piece = board.pieceAt(move.from());
        if (piece == null) {
            throw new IllegalArgumentException("No piece occupies the move's source square");
        }
        board.setPiece(move.from(), null);
        for (Position captured : move.captures()) {
            board.setPiece(captured, null);
        }
        Position destination = move.to();
        boolean reachesCrownRow = piece.side() == Side.BLACK
                ? destination.row() == Board.SIZE - 1
                : destination.row() == 0;
        board.setPiece(destination, reachesCrownRow ? piece.promote() : piece);
    }
}
