package com.tfdsimoes.checkers.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.ColourAssignment;
import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRulesTest {
    @Test
    void startingPositionBlackMovesFirstAndHasSevenMoves() {
        GameState game = new GameState();

        assertEquals(Side.BLACK, game.currentPlayer());
        assertEquals(7, game.legalMoves().size());
    }

    @Test
    void mandatoryCaptureRemovesAllSimpleMoves() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, false));
        board.setPiece(square(14), new Piece(Side.WHITE, false));

        List<Move> moves = MoveGenerator.legalMoves(board, Side.BLACK);

        assertEquals(List.of("9x18"), moves.stream().map(Move::notation).toList());
    }

    @Test
    void generatesCompleteMultiJumpSequenceAndAppliesEveryCapture() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, false));
        board.setPiece(square(14), new Piece(Side.WHITE, false));
        board.setPiece(square(22), new Piece(Side.WHITE, false));
        GameState game = new GameState(board, Side.BLACK);

        Move move = game.legalMoves().get(0);
        assertEquals("9x18x25", move.notation());
        game.makeMove(move);

        assertEquals(new Piece(Side.BLACK, false), game.board().pieceAt(square(25)));
        assertEquals(0, game.board().pieceCount(Side.WHITE));
        assertEquals(GameStatus.BLACK_WON, game.status());
    }

    @Test
    void multiJumpCannotCaptureTheSamePieceTwice() {
        Board board = new Board();
        board.setPiece(square(18), new Piece(Side.BLACK, true));
        board.setPiece(square(14), new Piece(Side.WHITE, false));

        List<Move> moves = MoveGenerator.legalMoves(board, Side.BLACK);

        assertEquals(List.of("18x9"), moves.stream().map(Move::notation).toList());
        assertEquals(List.of(square(14)), moves.get(0).captures());
    }

    @Test
    void promotionDuringCaptureEndsTheCaptureChain() {
        Board board = new Board();
        board.setPiece(square(21), new Piece(Side.BLACK, false));
        board.setPiece(square(25), new Piece(Side.WHITE, false));
        board.setPiece(square(26), new Piece(Side.WHITE, false));

        List<Move> moves = MoveGenerator.legalMoves(board, Side.BLACK);

        assertEquals(List.of("21x30"), moves.stream().map(Move::notation).toList());
    }

    @Test
    void simpleMovePromotesPieceAndKingsMoveBackward() {
        Board board = new Board();
        board.setPiece(square(25), new Piece(Side.BLACK, false));
        Move move = MoveGenerator.legalMoves(board, Side.BLACK).stream()
                .filter(candidate -> candidate.notation().equals("25-29"))
                .findFirst()
                .orElseThrow();
        MoveApplier.apply(board, move);

        assertEquals(new Piece(Side.BLACK, true), board.pieceAt(square(29)));
        assertTrue(MoveGenerator.legalMoves(board, Side.BLACK).stream()
                .anyMatch(candidate -> candidate.notation().equals("29-25")));
    }

    @Test
    void capturedKingCountsAsCaptured() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, false));
        board.setPiece(square(14), new Piece(Side.WHITE, true));

        Move move = MoveGenerator.legalMoves(board, Side.BLACK).get(0);
        MoveApplier.apply(board, move);

        assertEquals(0, board.pieceCount(Side.WHITE));
    }

    @Test
    void playerWithPiecesButNoLegalMoveLoses() {
        Board board = new Board();
        board.setPiece(square(25), new Piece(Side.BLACK, false));
        board.setPiece(square(1), new Piece(Side.WHITE, false));
        GameState game = new GameState(board, Side.BLACK);

        play(game, "25-29");

        assertEquals(1, game.board().pieceCount(Side.WHITE));
        assertTrue(MoveGenerator.legalMoves(game.board(), Side.WHITE).isEmpty());
        assertEquals(GameStatus.BLACK_WON, game.status());
    }

    @Test
    void playerWithoutAStartingMoveHasAlreadyLost() {
        Board board = new Board();
        board.setPiece(square(32), new Piece(Side.BLACK, false));
        board.setPiece(square(5), new Piece(Side.WHITE, false));
        GameState game = new GameState(board, Side.BLACK);

        assertEquals(GameStatus.WHITE_WON, game.status());
        assertTrue(game.legalMoves().isEmpty());
    }

    @Test
    void threefoldRepetitionRequiresSamePositionAndSideToMove() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, true));
        board.setPiece(square(24), new Piece(Side.WHITE, true));
        GameState game = new GameState(board, Side.BLACK);

        for (int cycle = 0; cycle < 2; cycle++) {
            play(game, "9-13");
            play(game, "24-19");
            play(game, "13-9");
            play(game, "19-24");
        }

        assertEquals(GameStatus.DRAW, game.status());
        assertEquals(8, game.noProgressPlies());
    }

    @Test
    void manMoveResetsNoProgressCounter() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, true));
        board.setPiece(square(1), new Piece(Side.BLACK, false));
        board.setPiece(square(24), new Piece(Side.WHITE, true));
        GameState game = new GameState(board, Side.BLACK);

        play(game, "9-13");
        play(game, "24-19");
        assertEquals(2, game.noProgressPlies());
        play(game, "1-5");

        assertEquals(0, game.noProgressPlies());
    }

    @Test
    void agreementEndsGameAndIllegalMovesAreRejected() {
        GameState game = new GameState();
        assertThrows(IllegalArgumentException.class, () -> game.makeMove(new Move(
                List.of(square(1), square(5)), List.of())));

        game.agreeToDraw();
        assertEquals(GameStatus.DRAW, game.status());
        assertTrue(game.legalMoves().isEmpty());
        assertThrows(IllegalStateException.class, game::agreeToDraw);
    }

    @Test
    void colourAssignmentSwapsBetweenGames() {
        ColourAssignment assignment = new ColourAssignment(Side.WHITE);

        assertEquals(Side.BLACK, assignment.secondPlayerSide());
        assertEquals(Side.BLACK, assignment.swappedForNextGame().firstPlayerSide());
        assertEquals(Side.WHITE,
                assignment.swappedForNextGame().swappedForNextGame().firstPlayerSide());
    }

    @Test
    void gameStateCopyIsIndependent() {
        GameState game = new GameState();
        GameState copy = game.copy();
        play(copy, "9-13");

        assertEquals(Side.BLACK, game.currentPlayer());
        assertEquals(Side.WHITE, copy.currentPlayer());
        assertNotEquals(game.history().size(), copy.history().size());
    }

    private static void play(GameState game, String notation) {
        Move move = game.legalMoves().stream()
                .filter(candidate -> candidate.notation().equals(notation))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected legal move " + notation));
        game.makeMove(move);
    }

    private static Position square(int number) {
        return Position.fromSquareNumber(number);
    }
}
