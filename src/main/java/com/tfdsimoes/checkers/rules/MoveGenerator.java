package com.tfdsimoes.checkers.rules;

import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MoveGenerator {
    private MoveGenerator() {
    }

    public static List<Move> legalMoves(Board board, Side side) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(side, "side");
        List<Move> captures = new ArrayList<>();
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            Position from = Position.fromSquareNumber(square);
            Piece piece = board.pieceAt(from);
            if (piece != null && piece.side() == side) {
                findCaptures(board, from, piece, new ArrayList<>(List.of(from)), new ArrayList<>(), captures);
            }
        }
        if (!captures.isEmpty()) {
            return List.copyOf(captures);
        }

        List<Move> moves = new ArrayList<>();
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            Position from = Position.fromSquareNumber(square);
            Piece piece = board.pieceAt(from);
            if (piece == null || piece.side() != side) {
                continue;
            }
            for (int rowDirection : rowDirections(piece)) {
                for (int columnDirection : new int[]{-1, 1}) {
                    Position to = offset(from, rowDirection, columnDirection);
                    if (to != null && board.pieceAt(to) == null) {
                        moves.add(new Move(List.of(from, to), List.of()));
                    }
                }
            }
        }
        return List.copyOf(moves);
    }

    private static void findCaptures(
            Board board,
            Position from,
            Piece piece,
            List<Position> path,
            List<Position> captured,
            List<Move> result) {
        boolean foundContinuation = false;
        for (int rowDirection : rowDirections(piece)) {
            for (int columnDirection : new int[]{-1, 1}) {
                Position jumped = offset(from, rowDirection, columnDirection);
                Position landing = offset(from, rowDirection * 2, columnDirection * 2);
                if (jumped == null || landing == null) {
                    continue;
                }
                Piece target = board.pieceAt(jumped);
                if (target == null || target.side() == piece.side() || captured.contains(jumped)
                        || board.pieceAt(landing) != null) {
                    continue;
                }

                foundContinuation = true;
                Board nextBoard = board.copy();
                nextBoard.setPiece(from, null);
                Piece movedPiece = promotedPiece(piece, landing);
                nextBoard.setPiece(landing, movedPiece);
                List<Position> nextPath = new ArrayList<>(path);
                nextPath.add(landing);
                List<Position> nextCaptured = new ArrayList<>(captured);
                nextCaptured.add(jumped);

                if (movedPiece.king() && !piece.king()) {
                    result.add(new Move(nextPath, nextCaptured));
                } else {
                    int resultSize = result.size();
                    findCaptures(nextBoard, landing, movedPiece, nextPath, nextCaptured, result);
                    if (result.size() == resultSize) {
                        result.add(new Move(nextPath, nextCaptured));
                    }
                }
            }
        }
        if (!foundContinuation && !captured.isEmpty()) {
            result.add(new Move(path, captured));
        }
    }

    private static Piece promotedPiece(Piece piece, Position destination) {
        boolean reachesCrownRow = piece.side() == Side.BLACK
                ? destination.row() == Board.SIZE - 1
                : destination.row() == 0;
        return reachesCrownRow ? piece.promote() : piece;
    }

    private static int[] rowDirections(Piece piece) {
        return piece.king() ? new int[]{-1, 1} : new int[]{piece.side().forwardDirection()};
    }

    private static Position offset(Position position, int rowDelta, int columnDelta) {
        int row = position.row() + rowDelta;
        int column = position.column() + columnDelta;
        if (row < 0 || row >= Board.SIZE || column < 0 || column >= Board.SIZE) {
            return null;
        }
        Position candidate = new Position(row, column);
        return candidate.isPlayable() ? candidate : null;
    }
}
