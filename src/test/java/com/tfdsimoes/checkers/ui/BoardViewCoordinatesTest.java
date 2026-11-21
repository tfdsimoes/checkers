package com.tfdsimoes.checkers.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import org.junit.jupiter.api.Test;

class BoardViewCoordinatesTest {
    @Test
    void mapsEveryDisplayedSquareAndFlippedSquareBackToTheModel() {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int column = 0; column < Board.SIZE; column++) {
                Position normal = BoardViewCoordinates.toBoardPosition(row, column, false);
                assertEquals(new Position(row, column), normal);
                assertEquals(new Position(row, column), BoardViewCoordinates.toViewPosition(normal, false));

                Position flipped = BoardViewCoordinates.toBoardPosition(row, column, true);
                assertEquals(new Position(Board.SIZE - 1 - row, Board.SIZE - 1 - column), flipped);
                assertEquals(new Position(row, column), BoardViewCoordinates.toViewPosition(flipped, true));
            }
        }
    }

    @Test
    void mapsPlayableSquareNumbersToTheirDisplayedPositions() {
        for (int squareNumber = 1; squareNumber <= Board.PLAYABLE_SQUARES; squareNumber++) {
            Position position = Position.fromSquareNumber(squareNumber);
            assertTrue(position.isPlayable());
            assertEquals(squareNumber, position.squareNumber());
        }
    }

    @Test
    void rejectsViewPositionsOutsideTheBoard() {
        assertThrows(IllegalArgumentException.class,
                () -> BoardViewCoordinates.toBoardPosition(-1, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> BoardViewCoordinates.toBoardPosition(0, Board.SIZE, true));
    }

    @Test
    void flipsOnlyForAWhiteHumanInPlayerVersusMachine() {
        assertTrue(BoardViewCoordinates.shouldFlipForWhitePlayer(
                GameMode.PLAYER_VS_MACHINE, Side.WHITE));
        assertFalse(BoardViewCoordinates.shouldFlipForWhitePlayer(
                GameMode.PLAYER_VS_MACHINE, Side.BLACK));
        assertFalse(BoardViewCoordinates.shouldFlipForWhitePlayer(
                GameMode.PLAYER_VS_PLAYER, Side.WHITE));
        assertFalse(BoardViewCoordinates.shouldFlipForWhitePlayer(
                GameMode.MACHINE_VS_MACHINE, Side.WHITE));
    }
}
