package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.util.SplittableRandom;

final class ZobristHasher {
    private static final long[][] PIECE_KEYS = new long[Board.PLAYABLE_SQUARES][4];
    private static final long SIDE_TO_MOVE_KEY;

    static {
        SplittableRandom random = new SplittableRandom(0x4C6F6E6753656564L);
        for (int square = 0; square < PIECE_KEYS.length; square++) {
            for (int piece = 0; piece < PIECE_KEYS[square].length; piece++) {
                PIECE_KEYS[square][piece] = random.nextLong();
            }
        }
        SIDE_TO_MOVE_KEY = random.nextLong();
    }

    private ZobristHasher() {
    }

    static long hash(GameState game) {
        Board board = game.board();
        long hash = game.currentPlayer() == Side.WHITE ? SIDE_TO_MOVE_KEY : 0L;
        for (int square = 1; square <= Board.PLAYABLE_SQUARES; square++) {
            Piece piece = board.pieceAt(Position.fromSquareNumber(square));
            if (piece == null) {
                continue;
            }
            int pieceIndex = (piece.side() == Side.BLACK ? 0 : 2) + (piece.king() ? 1 : 0);
            hash ^= PIECE_KEYS[square - 1][pieceIndex];
        }
        return hash ^ mix64(game.drawContextHash());
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
