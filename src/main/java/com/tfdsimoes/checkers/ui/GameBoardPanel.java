package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.Timer;

final class GameBoardPanel extends JPanel {
    private static final Color LIGHT_SQUARE = new Color(225, 202, 165);
    private static final Color DARK_SQUARE = new Color(112, 75, 53);
    private static final Color SELECTED_SQUARE = new Color(225, 190, 65);
    private static final Color LEGAL_DESTINATION = new Color(110, 180, 90);
    private static final Color COORDINATE_LABEL = new Color(80, 57, 39);
    private static final Color SQUARE_LABEL = new Color(245, 222, 184);
    private static final Color LAST_MOVE_HIGHLIGHT = new Color(242, 182, 59, 105);
    private static final Color CAPTURE_HIGHLIGHT = new Color(192, 61, 43, 125);
    private static final int ANIMATION_FRAME_DELAY_MS = 16;
    private static final long ANIMATION_SEGMENT_NANOS = 110_000_000L;

    private final GameState game;
    private final Consumer<Position> clickHandler;
    private Position selected;
    private List<Position> destinations = List.of();
    private boolean flipped;
    private Move lastMove;
    private Animation animation;
    private Timer animationTimer;

    GameBoardPanel(GameState game, Consumer<Position> clickHandler) {
        this.game = game;
        this.clickHandler = clickHandler;
        setPreferredSize(new Dimension(640, 640));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (animation != null) {
                    return;
                }
                int cell = cellSize();
                int left = boardLeft(cell);
                int top = boardTop(cell);
                int column = (event.getX() - left) / cell;
                int row = (event.getY() - top) / cell;
                if (event.getX() >= left && event.getY() >= top
                        && column >= 0 && column < Board.SIZE && row >= 0 && row < Board.SIZE) {
                    clickHandler.accept(BoardViewCoordinates.toBoardPosition(row, column, flipped));
                }
            }
        });
    }

    void setSelection(Position selected, List<Position> destinations) {
        this.selected = selected;
        this.destinations = List.copyOf(destinations);
        repaint();
    }

    void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    void setLastMove(Move move) {
        stopAnimation();
        lastMove = move;
        repaint();
    }

    void animateMove(Move move, Piece movingPiece) {
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(movingPiece, "movingPiece");
        stopAnimation();
        lastMove = move;
        animation = new Animation(move, movingPiece, System.nanoTime());
        animationTimer = new Timer(ANIMATION_FRAME_DELAY_MS, event -> {
            Animation active = animation;
            Timer timer = (Timer) event.getSource();
            if (active == null || System.nanoTime() - active.startedAtNanos() >= active.durationNanos()) {
                animation = null;
                timer.stop();
                if (animationTimer == timer) {
                    animationTimer = null;
                }
            }
            repaint();
        });
        animationTimer.setCoalesce(true);
        animationTimer.start();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cell = cellSize();
        int left = boardLeft(cell);
        int top = boardTop(cell);
        AnimationFrame animationFrame = currentAnimationFrame(left, top, cell);
        Board board = game.board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int column = 0; column < Board.SIZE; column++) {
                int x = left + column * cell;
                int y = top + row * cell;
                Position position = BoardViewCoordinates.toBoardPosition(row, column, flipped);
                paintSquare(g, position.isPlayable(), x, y, cell);
                paintHighlights(g, position, x, y, cell);
                if (position.equals(selected)) {
                    g.setColor(SELECTED_SQUARE);
                    g.fillRect(x, y, cell, cell);
                } else if (destinations.contains(position)) {
                    g.setColor(LEGAL_DESTINATION);
                    g.fillOval(x + cell / 3, y + cell / 3, cell / 3, cell / 3);
                }
                Piece piece = board.pieceAt(position);
                if (animation != null && position.equals(animation.move().to())) {
                    piece = null;
                }
                paintPiece(g, piece, x, y, cell);
                paintSquareNumber(g, position, x, y, cell);
            }
        }
        if (animationFrame != null) {
            paintPiece(g, animationFrame.piece(), (int) Math.round(animationFrame.x()),
                    (int) Math.round(animationFrame.y()), cell);
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawRect(left, top, cell * Board.SIZE, cell * Board.SIZE);
        paintCoordinates(g, left, top, cell, flipped);
        g.dispose();
    }

    private void paintHighlights(Graphics2D graphics, Position position, int x, int y, int cell) {
        if (lastMove == null) {
            return;
        }
        if (lastMove.path().contains(position)) {
            graphics.setColor(LAST_MOVE_HIGHLIGHT);
            graphics.fillRect(x, y, cell, cell);
        }
        if (lastMove.captures().contains(position)) {
            graphics.setColor(CAPTURE_HIGHLIGHT);
            graphics.fillRect(x, y, cell, cell);
        }
    }

    private void paintCoordinates(Graphics2D graphics, int left, int top, int cell, boolean boardFlipped) {
        int margin = coordinateMargin();
        int boardSize = cell * Board.SIZE;
        graphics.setColor(COORDINATE_LABEL);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, Math.min(16, cell / 5))));
        FontMetrics metrics = graphics.getFontMetrics();
        int topBaseline = top - (margin + metrics.getHeight()) / 2 + metrics.getDescent();
        int bottomBaseline = top + boardSize + (margin + metrics.getHeight()) / 2 - metrics.getDescent();
        int middleBaselineOffset = (cell + metrics.getAscent() - metrics.getDescent()) / 2;

        for (int column = 0; column < Board.SIZE; column++) {
            Position modelPosition = BoardViewCoordinates.toBoardPosition(0, column, boardFlipped);
            String label = Character.toString((char) ('A' + modelPosition.column()));
            int x = left + column * cell + (cell - metrics.stringWidth(label)) / 2;
            graphics.drawString(label, x, topBaseline);
            graphics.drawString(label, x, bottomBaseline);
        }
        for (int row = 0; row < Board.SIZE; row++) {
            Position modelPosition = BoardViewCoordinates.toBoardPosition(row, 0, boardFlipped);
            String label = Integer.toString(Board.SIZE - modelPosition.row());
            int y = top + row * cell + middleBaselineOffset;
            int labelWidth = metrics.stringWidth(label);
            graphics.drawString(label, left - (margin + labelWidth) / 2, y);
            graphics.drawString(label, left + boardSize + (margin - labelWidth) / 2, y);
        }
    }

    private static void paintSquare(Graphics2D graphics, boolean playable, int x, int y, int cell) {
        Color start = playable ? new Color(132, 91, 63) : new Color(246, 225, 190);
        Color end = playable ? DARK_SQUARE : LIGHT_SQUARE;
        graphics.setPaint(new GradientPaint(x, y, start, x + cell, y + cell, end));
        graphics.fillRect(x, y, cell, cell);
    }

    private static void paintSquareNumber(Graphics2D graphics, Position position, int x, int y, int cell) {
        if (!position.isPlayable()) {
            return;
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, cell / 10)));
        FontMetrics metrics = graphics.getFontMetrics();
        String label = Integer.toString(position.squareNumber());
        int labelX = x + cell / 16;
        int labelY = y + cell / 16;
        int padding = Math.max(2, cell / 40);
        int labelWidth = metrics.stringWidth(label);
        graphics.setColor(new Color(50, 35, 25, 210));
        graphics.fillRoundRect(labelX, labelY, labelWidth + padding * 2,
                metrics.getHeight() + padding, padding * 2, padding * 2);
        graphics.setColor(SQUARE_LABEL);
        graphics.drawString(label, labelX + padding, labelY + metrics.getAscent() + padding / 2);
    }

    private static void paintPiece(Graphics2D graphics, Piece piece, int x, int y, int cell) {
        if (piece == null) {
            return;
        }
        int margin = cell / 8;
        int diameter = cell - 2 * margin;
        graphics.setColor(new Color(25, 20, 16, 95));
        graphics.fillOval(x + margin + 2, y + margin + 4, diameter, diameter);
        Color highlight = piece.side() == Side.BLACK
                ? new Color(100, 100, 100)
                : new Color(255, 252, 240);
        Color shade = piece.side() == Side.BLACK
                ? new Color(25, 25, 25)
                : new Color(183, 162, 125);
        graphics.setPaint(new GradientPaint(x + margin, y + margin, highlight,
                x + cell - margin, y + cell - margin, shade));
        graphics.fillOval(x + margin, y + margin, diameter, diameter);
        graphics.setColor(piece.side() == Side.BLACK
                ? new Color(210, 204, 191)
                : new Color(97, 75, 51));
        graphics.setStroke(new BasicStroke(Math.max(2, cell / 40)));
        graphics.drawOval(x + margin + 2, y + margin + 2, diameter - 4, diameter - 4);
        if (piece.king()) {
            paintCrown(graphics, x, y, cell);
        }
    }

    private static void paintCrown(Graphics2D graphics, int x, int y, int cell) {
        float crownX = x + cell * 0.27f;
        float crownY = y + cell * 0.31f;
        float crownWidth = cell * 0.46f;
        float crownHeight = cell * 0.38f;
        Path2D crown = new Path2D.Float();
        crown.moveTo(crownX, crownY + crownHeight * 0.82f);
        crown.lineTo(crownX + crownWidth * 0.08f, crownY + crownHeight * 0.18f);
        crown.lineTo(crownX + crownWidth * 0.36f, crownY + crownHeight * 0.52f);
        crown.lineTo(crownX + crownWidth * 0.5f, crownY);
        crown.lineTo(crownX + crownWidth * 0.64f, crownY + crownHeight * 0.52f);
        crown.lineTo(crownX + crownWidth * 0.92f, crownY + crownHeight * 0.18f);
        crown.lineTo(crownX + crownWidth, crownY + crownHeight * 0.82f);
        crown.closePath();
        graphics.setColor(new Color(238, 190, 76));
        graphics.fill(crown);
        graphics.setColor(new Color(77, 52, 29));
        graphics.setStroke(new BasicStroke(Math.max(1, cell / 45)));
        graphics.draw(crown);
        graphics.drawLine((int) (crownX + crownWidth * 0.12f), (int) (crownY + crownHeight * 0.9f),
                (int) (crownX + crownWidth * 0.88f), (int) (crownY + crownHeight * 0.9f));
    }

    private AnimationFrame currentAnimationFrame(int left, int top, int cell) {
        if (animation == null) {
            return null;
        }
        long elapsed = System.nanoTime() - animation.startedAtNanos();
        if (elapsed >= animation.durationNanos()) {
            stopAnimation();
            return null;
        }
        int segment = (int) (elapsed / ANIMATION_SEGMENT_NANOS);
        double progress = (elapsed % ANIMATION_SEGMENT_NANOS) / (double) ANIMATION_SEGMENT_NANOS;
        Position from = BoardViewCoordinates.toViewPosition(animation.move().path().get(segment), flipped);
        Position to = BoardViewCoordinates.toViewPosition(animation.move().path().get(segment + 1), flipped);
        double x = left + (from.column() + (to.column() - from.column()) * progress) * cell;
        double y = top + (from.row() + (to.row() - from.row()) * progress) * cell;
        return new AnimationFrame(animation.movingPiece(), x, y);
    }

    private void stopAnimation() {
        animation = null;
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private int cellSize() {
        return Math.max(1, (Math.min(getWidth(), getHeight()) - 2 * coordinateMargin()) / Board.SIZE);
    }

    private int coordinateMargin() {
        return Math.max(18, Math.min(getWidth(), getHeight()) / 24);
    }

    private int boardLeft(int cell) {
        return (getWidth() - cell * Board.SIZE) / 2;
    }

    private int boardTop(int cell) {
        return (getHeight() - cell * Board.SIZE) / 2;
    }

    private record Animation(Move move, Piece movingPiece, long startedAtNanos) {
        private long durationNanos() {
            return ANIMATION_SEGMENT_NANOS * (move.path().size() - 1);
        }
    }

    private record AnimationFrame(Piece piece, double x, double y) {
    }
}
