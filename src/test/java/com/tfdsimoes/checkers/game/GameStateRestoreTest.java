package com.tfdsimoes.checkers.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStateRestoreTest {
    @Test
    void restoresBoardTurnAndHistory() {
        GameState game = new GameState();
        play(game, "9-13");
        GameState snapshot = game.copy();
        play(game, "22-18");

        game.restoreFrom(snapshot);

        assertEquals(Side.WHITE, game.currentPlayer());
        assertEquals(1, game.history().size());
        assertEquals(new Piece(Side.BLACK, false), game.board().pieceAt(square(13)));
        assertNull(game.board().pieceAt(square(18)));
    }

    @Test
    void restoresDrawCountersAndRepetitionHistory() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, true));
        board.setPiece(square(24), new Piece(Side.WHITE, true));
        GameState game = new GameState(board, Side.BLACK);
        for (int cycle = 0; cycle < 2; cycle++) {
            play(game, "9-13");
            play(game, "24-19");
            play(game, "13-9");
            if (cycle == 1) {
                GameState beforeFinalMove = game.copy();
                play(game, "19-24");
                assertEquals(GameStatus.DRAW, game.status());
                game.restoreFrom(beforeFinalMove);
                assertEquals(GameStatus.IN_PROGRESS, game.status());
                assertEquals(7, game.noProgressPlies());
                play(game, "19-24");
                assertEquals(GameStatus.DRAW, game.status());
            } else {
                play(game, "19-24");
            }
        }
    }

    @Test
    void undoPolicyCountsHumanPliesAndKeepsAiOpening() {
        assertEquals(1, UndoPolicy.movesToUndo(GameMode.PLAYER_VS_PLAYER, 1, Side.BLACK));
        assertEquals(2, UndoPolicy.movesToUndo(GameMode.PLAYER_VS_MACHINE, 4, Side.BLACK));
        assertEquals(2, UndoPolicy.movesToUndo(GameMode.PLAYER_VS_MACHINE, 3, Side.WHITE));
        assertEquals(0, UndoPolicy.movesToUndo(GameMode.PLAYER_VS_MACHINE, 1, Side.WHITE));
        assertEquals(0, UndoPolicy.movesToUndo(GameMode.MACHINE_VS_MACHINE, 4, null));
        assertThrows(IllegalArgumentException.class,
                () -> UndoPolicy.movesToUndo(GameMode.PLAYER_VS_PLAYER, -1, Side.BLACK));
    }

    @Test
    void noProgressDrawOccursAtEightyPlies() {
        Board board = new Board();
        board.setPiece(square(9), new Piece(Side.BLACK, true));
        board.setPiece(square(24), new Piece(Side.WHITE, true));
        GameState game = new GameState(board, Side.BLACK, GameState.NO_PROGRESS_DRAW_PLIES - 1);

        play(game, "9-13");

        assertEquals(GameStatus.DRAW, game.status());
        assertEquals(GameState.NO_PROGRESS_DRAW_PLIES, game.noProgressPlies());
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
