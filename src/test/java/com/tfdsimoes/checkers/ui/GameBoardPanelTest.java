package com.tfdsimoes.checkers.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Position;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameBoardPanelTest {
    @Test
    void mouseClicksMapThroughTheFlippedBoardCoordinates() {
        List<Position> clickedPositions = new ArrayList<>();
        GameBoardPanel panel = new GameBoardPanel(new GameState(), clickedPositions::add);
        panel.setSize(640, 640);
        panel.setFlipped(true);

        clickViewSquare(panel, 0, 0);

        assertEquals(List.of(new Position(Board.SIZE - 1, Board.SIZE - 1)), clickedPositions);
    }

    private static void clickViewSquare(GameBoardPanel panel, int row, int column) {
        int cell = (640 - 2 * (640 / 24)) / Board.SIZE;
        int left = (640 - cell * Board.SIZE) / 2;
        int top = left;
        int x = left + column * cell + cell / 2;
        int y = top + row * cell + cell / 2;
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false));
    }
}
