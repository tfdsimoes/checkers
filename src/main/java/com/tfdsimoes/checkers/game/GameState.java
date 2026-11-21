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

public final class GameState {
    public static final int NO_PROGRESS_DRAW_PLIES = 80;

    private final Board board;
    private final List<Move> history;
    private final Map<Long, Integer> repetitions;
    private Side currentPlayer;
    private GameStatus status;
    private int noProgressPlies;

    public GameState() {
        this(Board.startingPosition(), Side.BLACK);
    }

    public GameState(Board board, Side currentPlayer) {
        this(board, currentPlayer, 0);
    }

    GameState(Board board, Side currentPlayer, int noProgressPlies) {
        this(board.copy(), new ArrayList<>(), new HashMap<>(),
                Objects.requireNonNull(currentPlayer, "currentPlayer"), GameStatus.IN_PROGRESS,
                validateNoProgressPlies(noProgressPlies));
        recordPosition();
        if (this.board.pieceCount(this.currentPlayer) == 0
                || MoveGenerator.legalMoves(this.board, this.currentPlayer).isEmpty()) {
            this.status = this.currentPlayer == Side.BLACK ? GameStatus.WHITE_WON : GameStatus.BLACK_WON;
        }
    }

    private GameState(
            Board board,
            List<Move> history,
            Map<Long, Integer> repetitions,
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

    Board boardForHash() {
        return board;
    }

    public Piece pieceAt(Position position) {
        return board.pieceAt(Objects.requireNonNull(position, "position"));
    }

    public int mobilityDifference() {
        return MoveGenerator.mobilityDifference(board);
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
        for (Map.Entry<Long, Integer> repetition : repetitions.entrySet()) {
            long entry = repetition.getKey() ^ mix64(repetition.getValue());
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
        applyGeneratedMove(move);
    }

    /** Applies a move already produced by {@link #legalMoves()} without regenerating legal moves. */
    public void makeGeneratedMove(Move move) {
        Objects.requireNonNull(move, "move");
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("The game is already over");
        }
        applyGeneratedMove(move);
    }

    private void applyGeneratedMove(Move move) {
        Piece movingPiece = board.pieceAt(move.from());
        MoveApplier.apply(board, move);
        history.add(move);
        noProgressPlies = move.isCapture() || !movingPiece.king() ? 0 : noProgressPlies + 1;
        Side mover = currentPlayer;
        currentPlayer = currentPlayer.opposite();
        recordPosition();

        if (board.pieceCount(currentPlayer) == 0 || MoveGenerator.legalMoves(board, currentPlayer).isEmpty()) {
            status = mover == Side.BLACK ? GameStatus.BLACK_WON : GameStatus.WHITE_WON;
        } else if (repetitions.get(positionHash()) >= 3 || noProgressPlies >= NO_PROGRESS_DRAW_PLIES) {
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

    public GameState searchCopy() {
        return new GameState(board.copy(), new ArrayList<>(), new HashMap<>(repetitions),
                currentPlayer, status, noProgressPlies);
    }

    public void restoreFrom(GameState snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot == this) {
            return;
        }
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            Position position = Position.fromSquareNumber(square);
            board.setPiece(position, snapshot.board.pieceAt(position));
        }
        history.clear();
        history.addAll(snapshot.history);
        repetitions.clear();
        repetitions.putAll(snapshot.repetitions);
        currentPlayer = snapshot.currentPlayer;
        status = snapshot.status;
        noProgressPlies = snapshot.noProgressPlies;
    }

    private void recordPosition() {
        repetitions.merge(positionHash(), 1, Integer::sum);
    }

    private long positionHash() {
        return ZobristHasher.positionHash(board, currentPlayer);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static int validateNoProgressPlies(int noProgressPlies) {
        if (noProgressPlies < 0 || noProgressPlies >= NO_PROGRESS_DRAW_PLIES) {
            throw new IllegalArgumentException("Initial no-progress plies must be between 0 and 79");
        }
        return noProgressPlies;
    }
}
