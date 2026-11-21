package com.tfdsimoes.checkers.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSidebarPanelTest {
    @Test
    void summarizesMaterialAndStandardSimpleMoveNotation() {
        GameState game = new GameState();
        Move move = game.legalMoves().stream()
                .filter(candidate -> candidate.notation().equals("9-13"))
                .findFirst()
                .orElseThrow();
        game.makeMove(move);

        GameSidebarPanel.SidebarSummary summary = GameSidebarPanel.summarize(game);

        assertEquals(12, summary.blackPieces());
        assertEquals(12, summary.whitePieces());
        assertEquals(List.of("1. 9-13"), summary.moveHistory());
    }

    @Test
    void summarizesCaptureNotationAndUpdatedMaterialCounts() {
        Board board = new Board();
        board.setPiece(Position.fromSquareNumber(9), new Piece(Side.BLACK, false));
        board.setPiece(Position.fromSquareNumber(14), new Piece(Side.WHITE, false));
        board.setPiece(Position.fromSquareNumber(22), new Piece(Side.WHITE, false));
        GameState game = new GameState(board, Side.BLACK);
        Move move = game.legalMoves().get(0);
        game.makeMove(move);

        GameSidebarPanel.SidebarSummary summary = GameSidebarPanel.summarize(game);

        assertEquals(1, summary.blackPieces());
        assertEquals(0, summary.whitePieces());
        assertEquals(List.of("1. 9x18x25"), summary.moveHistory());
    }
}
