package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.Objects;

public final class EvaluationFunction {
    private static final int WIN_SCORE = 100_000;

    public int evaluate(GameState game, Side perspective) {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(perspective, "perspective");
        GameStatus status = game.status();
        if (status != GameStatus.IN_PROGRESS) {
            if (status == GameStatus.DRAW) {
                return 0;
            }
            Side winner = status == GameStatus.BLACK_WON ? Side.BLACK : Side.WHITE;
            return winner == perspective ? WIN_SCORE : -WIN_SCORE;
        }

        Board board = game.board();
        int score = 0;
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            Position position = Position.fromSquareNumber(square);
            Piece piece = board.pieceAt(position);
            if (piece == null) {
                continue;
            }
            int pieceScore = piece.king() ? 175 : 100;
            if (!piece.king()) {
                pieceScore += piece.side() == Side.BLACK
                        ? position.row() * 4
                        : (Board.SIZE - 1 - position.row()) * 4;
                boolean protectsBackRow = piece.side() == Side.BLACK
                        ? position.row() == 0
                        : position.row() == Board.SIZE - 1;
                if (protectsBackRow) {
                    pieceScore += 6;
                }
            }
            if (position.row() >= 2 && position.row() <= 5
                    && position.column() >= 2 && position.column() <= 5) {
                pieceScore += 4;
            }
            score += piece.side() == Side.BLACK ? pieceScore : -pieceScore;
        }

        score += (game.legalMovesFor(Side.BLACK) - game.legalMovesFor(Side.WHITE)) * 2;
        return perspective == Side.BLACK ? score : -score;
    }
}
