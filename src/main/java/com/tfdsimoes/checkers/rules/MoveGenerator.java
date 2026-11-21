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
    private static final int[] BLACK_ROW_DIRECTIONS = {1};
    private static final int[] WHITE_ROW_DIRECTIONS = {-1};
    private static final int[] KING_ROW_DIRECTIONS = {-1, 1};
    private static final int[] COLUMN_DIRECTIONS = {-1, 1};

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
                for (int columnDirection : COLUMN_DIRECTIONS) {
                    Position to = offset(from, rowDirection, columnDirection);
                    if (to != null && board.pieceAt(to) == null) {
                        moves.add(new Move(List.of(from, to), List.of()));
                    }
                }
            }
        }
        return List.copyOf(moves);
    }

    public static int mobilityDifference(Board board) {
        Objects.requireNonNull(board, "board");
        List<Position> blackPieces = new ArrayList<>();
        List<Position> whitePieces = new ArrayList<>();
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            Position position = Position.fromSquareNumber(square);
            Piece piece = board.pieceAt(position);
            if (piece != null) {
                (piece.side() == Side.BLACK ? blackPieces : whitePieces).add(position);
            }
        }
        return mobility(board, blackPieces) - mobility(board, whitePieces);
    }

    private static int mobility(Board board, List<Position> pieces) {
        int captureCount = 0;
        for (Position from : pieces) {
            Piece piece = board.pieceAt(from);
            captureCount += countCaptures(board, from, piece, new boolean[Board.PLAYABLE_SQUARES], 0);
        }
        if (captureCount > 0) {
            return captureCount;
        }

        int moveCount = 0;
        for (Position from : pieces) {
            Piece piece = board.pieceAt(from);
            for (int rowDirection : rowDirections(piece)) {
                for (int columnDirection : COLUMN_DIRECTIONS) {
                    Position to = offset(from, rowDirection, columnDirection);
                    if (to != null && board.pieceAt(to) == null) {
                        moveCount++;
                    }
                }
            }
        }
        return moveCount;
    }

    private static int countCaptures(
            Board board,
            Position from,
            Piece piece,
            boolean[] captured,
            int capturesSoFar) {
        int captureCount = 0;
        boolean foundContinuation = false;
        for (int rowDirection : rowDirections(piece)) {
            for (int columnDirection : COLUMN_DIRECTIONS) {
                Position jumped = offset(from, rowDirection, columnDirection);
                Position landing = offset(from, rowDirection * 2, columnDirection * 2);
                if (jumped == null || landing == null) {
                    continue;
                }
                int capturedIndex = jumped.squareNumber() - 1;
                Piece target = board.pieceAt(jumped);
                if (target == null || target.side() == piece.side() || captured[capturedIndex]
                        || board.pieceAt(landing) != null) {
                    continue;
                }

                foundContinuation = true;
                Piece movedPiece = promotedPiece(piece, landing);
                board.setPiece(from, null);
                board.setPiece(landing, movedPiece);
                captured[capturedIndex] = true;
                if (movedPiece.king() && !piece.king()) {
                    captureCount++;
                } else {
                    int continuations = countCaptures(board, landing, movedPiece, captured, capturesSoFar + 1);
                    captureCount += continuations == 0 ? 1 : continuations;
                }
                captured[capturedIndex] = false;
                board.setPiece(landing, null);
                board.setPiece(from, piece);
            }
        }
        return !foundContinuation && capturesSoFar > 0 ? 1 : captureCount;
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
            for (int columnDirection : COLUMN_DIRECTIONS) {
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
                Piece movedPiece = promotedPiece(piece, landing);
                board.setPiece(from, null);
                board.setPiece(landing, movedPiece);
                List<Position> nextPath = new ArrayList<>(path);
                nextPath.add(landing);
                List<Position> nextCaptured = new ArrayList<>(captured);
                nextCaptured.add(jumped);

                if (movedPiece.king() && !piece.king()) {
                    result.add(new Move(nextPath, nextCaptured));
                } else {
                    int resultSize = result.size();
                    findCaptures(board, landing, movedPiece, nextPath, nextCaptured, result);
                    if (result.size() == resultSize) {
                        result.add(new Move(nextPath, nextCaptured));
                    }
                }
                board.setPiece(landing, null);
                board.setPiece(from, piece);
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
        if (piece.king()) {
            return KING_ROW_DIRECTIONS;
        }
        return piece.side() == Side.BLACK ? BLACK_ROW_DIRECTIONS : WHITE_ROW_DIRECTIONS;
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
