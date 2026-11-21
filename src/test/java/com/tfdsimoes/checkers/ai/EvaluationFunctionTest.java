package com.tfdsimoes.checkers.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import org.junit.jupiter.api.Test;

class EvaluationFunctionTest {
    @Test
    void mateInOneScoresAboveMateInThree() {
        Board board = new Board();
        board.setPiece(Position.fromSquareNumber(1), new Piece(Side.WHITE, false));
        GameState blackWin = new GameState(board, Side.WHITE);
        EvaluationFunction evaluation = new EvaluationFunction();

        int mateInOne = evaluation.evaluate(blackWin, Side.BLACK, 1);
        int mateInThree = evaluation.evaluate(blackWin, Side.BLACK, 3);

        assertEquals(EvaluationFunction.WIN_SCORE - 1, mateInOne);
        assertEquals(EvaluationFunction.WIN_SCORE - 3, mateInThree);
        assertTrue(mateInOne > mateInThree);
    }
}
