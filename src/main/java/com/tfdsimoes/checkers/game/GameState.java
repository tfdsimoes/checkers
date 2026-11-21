package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import com.tfdsimoes.checkers.rules.MoveApplier;
import com.tfdsimoes.checkers.rules.MoveGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GameState {
    public static final int NO_PROGRESS_DRAW_PLIES = 80;

    private final Board board;
    private final List<Move> history;
    private final Map<String, Integer> repetitions;
    private Side currentPlayer;
    private GameStatus status;
    private int noProgressPlies;

    public GameState() {
        this(Board.startingPosition(), Side.BLACK);
    }

    public GameState(Board board, Side currentPlayer) {
        this(board.copy(), new ArrayList<>(), new HashMap<>(),
                Objects.requireNonNull(currentPlayer, "currentPlayer"), GameStatus.IN_PROGRESS, 0);
        recordPosition();
    }

    private GameState(
            Board board,
            List<Move> history,
            Map<String, Integer> repetitions,
            Side currentPlayer,
            GameStatus status,
            int noProgressPlies) {
        this.board = board;
        this.history = history;
        this.repetitions = repetitions;
        this.currentPlayer = currentPlayer;
        this.status = status;
        this.noProgressPlies = noProgressPlies;
    }

    public Board board() {
        return board.copy();
    }

    public Side currentPlayer() {
        return currentPlayer;
    }

    public GameStatus status() {
        return status;
    }

    public int noProgressPlies() {
        return noProgressPlies;
    }

    public long drawContextHash() {
        long hash = noProgressPlies;
        for (Map.Entry<String, Integer> repetition : repetitions.entrySet()) {
            long entry = ((long) repetition.getKey().hashCode() << 32) ^ repetition.getValue();
            hash ^= mix64(entry);
        }
        return hash;
    }

    public List<Move> history() {
        return List.copyOf(history);
    }

    public List<Move> legalMoves() {
        if (status != GameStatus.IN_PROGRESS) {
            return List.of();
        }
        return MoveGenerator.legalMoves(board, currentPlayer);
    }

    public int legalMovesFor(Side side) {
        return MoveGenerator.legalMoves(board, Objects.requireNonNull(side, "side")).size();
    }

    public void makeMove(Move move) {
        Objects.requireNonNull(move, "move");
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("The game is already over");
        }
        if (!legalMoves().contains(move)) {
            throw new IllegalArgumentException("Move is not legal in the current position");
        }

        Piece movingPiece = board.pieceAt(move.from());
        MoveApplier.apply(board, move);
        history.add(move);
        noProgressPlies = move.isCapture() || !movingPiece.king() ? 0 : noProgressPlies + 1;
        Side mover = currentPlayer;
        currentPlayer = currentPlayer.opposite();
        recordPosition();

        if (board.pieceCount(currentPlayer) == 0 || MoveGenerator.legalMoves(board, currentPlayer).isEmpty()) {
            status = mover == Side.BLACK ? GameStatus.BLACK_WON : GameStatus.WHITE_WON;
        } else if (repetitions.get(positionKey()) >= 3 || noProgressPlies >= NO_PROGRESS_DRAW_PLIES) {
            status = GameStatus.DRAW;
        }
    }

    public void agreeToDraw() {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("The game is already over");
        }
        status = GameStatus.DRAW;
    }

    public GameState copy() {
        return new GameState(board.copy(), new ArrayList<>(history), new HashMap<>(repetitions),
                currentPlayer, status, noProgressPlies);
    }

    private void recordPosition() {
        repetitions.merge(positionKey(), 1, Integer::sum);
    }

    private String positionKey() {
        String pieces = java.util.stream.IntStream.rangeClosed(1, Board.PLAYABLE_SQUARES)
                .mapToObj(square -> {
                    Piece piece = board.pieceAt(Position.fromSquareNumber(square));
                    if (piece == null) {
                        return "-";
                    }
                    return (piece.side() == Side.BLACK ? "b" : "w") + (piece.king() ? "k" : "m");
                })
                .collect(Collectors.joining());
        return currentPlayer.name() + pieces;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
