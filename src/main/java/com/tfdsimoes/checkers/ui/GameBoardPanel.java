package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;

final class GameBoardPanel extends JPanel {
    private static final Color LIGHT_SQUARE = new Color(238, 216, 181);
    private static final Color DARK_SQUARE = new Color(105, 73, 52);
    private static final Color SELECTED_SQUARE = new Color(225, 190, 65);
    private static final Color LEGAL_DESTINATION = new Color(110, 180, 90);

    private final GameState game;
    private final Consumer<Position> clickHandler;
    private Position selected;
    private List<Position> destinations = List.of();

    GameBoardPanel(GameState game, Consumer<Position> clickHandler) {
        this.game = game;
        this.clickHandler = clickHandler;
        setPreferredSize(new Dimension(640, 640));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                int cell = cellSize();
                int left = (getWidth() - cell * Board.SIZE) / 2;
                int top = (getHeight() - cell * Board.SIZE) / 2;
                int column = (event.getX() - left) / cell;
                int row = (event.getY() - top) / cell;
                if (event.getX() >= left && event.getY() >= top
                        && column >= 0 && column < Board.SIZE && row >= 0 && row < Board.SIZE) {
                    clickHandler.accept(new Position(row, column));
                }
            }
        });
    }

    void setSelection(Position selected, List<Position> destinations) {
        this.selected = selected;
        this.destinations = List.copyOf(destinations);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cell = cellSize();
        int left = (getWidth() - cell * Board.SIZE) / 2;
        int top = (getHeight() - cell * Board.SIZE) / 2;
        Board board = game.board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int column = 0; column < Board.SIZE; column++) {
                int x = left + column * cell;
                int y = top + row * cell;
                Position position = new Position(row, column);
                g.setColor(position.isPlayable() ? DARK_SQUARE : LIGHT_SQUARE);
                g.fillRect(x, y, cell, cell);
                if (position.equals(selected)) {
                    g.setColor(SELECTED_SQUARE);
                    g.fillRect(x, y, cell, cell);
                } else if (destinations.contains(position)) {
                    g.setColor(LEGAL_DESTINATION);
                    g.fillOval(x + cell / 3, y + cell / 3, cell / 3, cell / 3);
                }
                paintPiece(g, board.pieceAt(position), x, y, cell);
            }
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawRect(left, top, cell * Board.SIZE, cell * Board.SIZE);
        g.dispose();
    }

    private static void paintPiece(Graphics2D graphics, Piece piece, int x, int y, int cell) {
        if (piece == null) {
            return;
        }
        int margin = cell / 8;
        graphics.setColor(piece.side() == com.tfdsimoes.checkers.model.Side.BLACK
                ? new Color(40, 40, 40)
                : new Color(245, 245, 235));
        graphics.fillOval(x + margin, y + margin, cell - 2 * margin, cell - 2 * margin);
        graphics.setColor(piece.side() == com.tfdsimoes.checkers.model.Side.BLACK
                ? new Color(220, 220, 220)
                : new Color(65, 65, 65));
        graphics.setStroke(new BasicStroke(Math.max(2, cell / 40)));
        graphics.drawOval(x + margin + 2, y + margin + 2, cell - 2 * margin - 4, cell - 2 * margin - 4);
        if (piece.king()) {
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, cell / 3)));
            graphics.drawString("K", x + cell / 2 - cell / 10, y + cell * 2 / 3);
        }
    }

    private int cellSize() {
        return Math.max(1, Math.min(getWidth(), getHeight()) / Board.SIZE);
    }
}
