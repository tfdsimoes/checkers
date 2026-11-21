package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameConfiguration;
import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.game.GameSetup;
import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.game.PlayerController;
import com.tfdsimoes.checkers.game.UndoPolicy;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

final class GameSetupWindow extends JFrame {
    private final GameSetup setup;
    private final Consumer<GameConfiguration> returnHandler;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JButton pauseButton = new JButton("Pause");
    private final JButton undoButton = new JButton("Undo");
    private final JButton offerDrawButton = new JButton("Offer draw");
    private final JButton passDrawButton = new JButton();
    private final JButton agreeDrawButton = new JButton("Agree to draw");
    private final JButton declineDrawButton = new JButton("Decline");
    private final GameBoardPanel boardPanel;
    private final GameSidebarPanel sidebarPanel = new GameSidebarPanel();
    private final GamePlayerCard blackPlayerCard = new GamePlayerCard(Side.BLACK);
    private final GamePlayerCard whitePlayerCard = new GamePlayerCard(Side.WHITE);
    private SwingWorker<Move, Void> aiWorker;
    private final List<GameState> undoSnapshots = new ArrayList<>();
    private List<Move> selectedMoves = List.of();
    private Position selectedSource;
    private int selectedPathIndex;
    private Side drawOfferBy;
    private boolean drawResponseReady;
    private boolean paused;
    private boolean returning;
    private boolean started;
    private int aiTurnGeneration;

    GameSetupWindow(GameConfiguration configuration, Consumer<GameConfiguration> returnHandler) {
        super("Checkers");
        this.setup = GameSetup.create(configuration);
        this.returnHandler = returnHandler;
        this.boardPanel = new GameBoardPanel(setup.game(), this::handleBoardClick);
        this.boardPanel.setFlipped(BoardViewCoordinates.shouldFlipForWhitePlayer(
                configuration.mode(), configuration.firstPlayerSide()));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                returnToMenu();
            }
        });
        status.setOpaque(true);
        status.setBackground(new Color(38, 53, 73));
        status.setForeground(Color.WHITE);
        status.setBorder(new EmptyBorder(10, 12, 10, 12));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));

        JPanel playerCards = new JPanel(new GridLayout(1, 2, 8, 0));
        playerCards.add(blackPlayerCard);
        playerCards.add(whitePlayerCard);
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.add(status, BorderLayout.NORTH);
        header.add(playerCards, BorderLayout.CENTER);

        setLayout(new BorderLayout(8, 8));
        add(header, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(sidebarPanel, BorderLayout.EAST);

        JButton menuButton = new JButton("Menu");
        configureToolbarButton(pauseButton, "Pause", ToolbarGlyph.PAUSE);
        configureToolbarButton(undoButton, "Undo", ToolbarGlyph.UNDO);
        configureToolbarButton(offerDrawButton, "Offer draw", ToolbarGlyph.OFFER);
        configureToolbarButton(passDrawButton, "Pass to opponent", ToolbarGlyph.HANDOFF);
        configureToolbarButton(agreeDrawButton, "Agree to draw", ToolbarGlyph.ACCEPT);
        configureToolbarButton(declineDrawButton, "Decline", ToolbarGlyph.DECLINE);
        configureToolbarButton(menuButton, "Menu", ToolbarGlyph.MENU);
        menuButton.addActionListener(event -> returnToMenu());
        pauseButton.addActionListener(event -> togglePause());
        undoButton.addActionListener(event -> undo());
        offerDrawButton.addActionListener(event -> offerDraw());
        passDrawButton.addActionListener(event -> passDrawToOpponent());
        agreeDrawButton.addActionListener(event -> agreeToDraw());
        declineDrawButton.addActionListener(event -> declineDraw());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controls.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        controls.add(pauseButton);
        controls.add(undoButton);
        controls.add(offerDrawButton);
        controls.add(passDrawButton);
        controls.add(agreeDrawButton);
        controls.add(declineDrawButton);
        controls.add(menuButton);
        add(controls, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        refresh();
    }

    void start() {
        if (started) {
            throw new IllegalStateException("Game window has already started");
        }
        started = true;
        startAiTurn();
    }

    private void handleBoardClick(Position position) {
        GameState game = setup.game();
        if (paused || drawOfferBy != null || game.status() != GameStatus.IN_PROGRESS
                || !setup.controllerFor(game.currentPlayer()).isHuman() || aiWorker != null) {
            return;
        }

        if (selectedSource == null) {
            selectSource(position);
            return;
        }

        List<Move> nextCandidates = selectedMoves.stream()
                .filter(move -> move.path().size() > selectedPathIndex + 1
                        && move.path().get(selectedPathIndex + 1).equals(position))
                .toList();
        if (nextCandidates.isEmpty()) {
            if (position.equals(selectedSource)) {
                clearSelection();
            } else {
                selectSource(position);
                status.setText("That move is not legal; choose a highlighted destination.");
            }
            return;
        }

        selectedMoves = nextCandidates;
        selectedPathIndex++;
        if (selectedMoves.stream().allMatch(move -> move.path().size() == selectedPathIndex + 1)) {
            makeMove(selectedMoves.get(0));
            clearSelection();
            refresh();
            startAiTurn();
        } else {
            refreshSelection();
        }
    }

    private void selectSource(Position position) {
        Piece piece = setup.game().board().pieceAt(position);
        if (piece == null || piece.side() != setup.game().currentPlayer()) {
            clearSelection();
            status.setText("Select one of your pieces.");
            return;
        }
        List<Move> moves = setup.game().legalMoves().stream()
                .filter(move -> move.from().equals(position))
                .toList();
        if (moves.isEmpty()) {
            clearSelection();
            status.setText("That piece has no legal move.");
            return;
        }
        selectedSource = position;
        selectedMoves = moves;
        selectedPathIndex = 0;
        refreshSelection();
    }

    private void refreshSelection() {
        List<Position> destinations = selectedMoves.stream()
                .filter(move -> move.path().size() > selectedPathIndex + 1)
                .map(move -> move.path().get(selectedPathIndex + 1))
                .distinct()
                .toList();
        Position currentPosition = selectedMoves.get(0).path().get(selectedPathIndex);
        boardPanel.setSelection(currentPosition, destinations);
    }

    private void clearSelection() {
        selectedSource = null;
        selectedMoves = List.of();
        selectedPathIndex = 0;
        boardPanel.setSelection(null, List.of());
    }

    private void togglePause() {
        paused = !paused;
        aiTurnGeneration++;
        if (paused && aiWorker != null) {
            aiWorker.cancel(true);
        }
        if (!paused) {
            startAiTurn();
        }
        refresh();
    }

    private void startAiTurn() {
        GameState game = setup.game();
        if (!started || !GameWindowPolicy.canStartAiTurn(
                returning,
                paused,
                game.status(),
                aiWorker != null,
                setup.controllerFor(game.currentPlayer()).isHuman())) {
            return;
        }
        PlayerController controller = setup.controllerFor(game.currentPlayer());
        int generation = ++aiTurnGeneration;
        aiWorker = new SwingWorker<>() {
            @Override
            protected Move doInBackground() throws Exception {
                Thread.sleep(250);
                return controller.chooseMove(game);
            }

            @Override
            protected void done() {
                if (aiWorker == this) {
                    aiWorker = null;
                }
                if (returning) {
                    return;
                }
                if (generation != aiTurnGeneration || paused) {
                    if (!paused) {
                        startAiTurn();
                    }
                    return;
                }
                try {
                    makeMove(get());
                    clearSelection();
                    refresh();
                    startAiTurn();
                } catch (CancellationException exception) {
                    displayAiError("AI move was cancelled.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    displayAiError("AI move was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    displayAiError("AI failed: " + (cause == null ? exception.getMessage() : cause.getMessage()));
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    displayAiError("AI returned an invalid move: " + exception.getMessage());
                }
            }
        };
        refresh();
        aiWorker.execute();
    }

    private void displayAiError(String message) {
        refresh();
        status.setText(message);
    }

    private void refresh() {
        GameState game = setup.game();
        sidebarPanel.update(game);
        String context = setup.configuration().mode() + " | seed " + setup.configuration().seed();
        if (game.status() == GameStatus.IN_PROGRESS) {
            Side side = game.currentPlayer();
            String player = setup.controllerFor(side).displayName();
            String turn = paused ? "Paused" : side + " to move (" + player + ")";
            String lastMove = game.history().isEmpty()
                    ? ""
                    : " | Last move: " + game.history().get(game.history().size() - 1).notation();
            status.setText(context + " | " + turn + lastMove);
        } else {
            String result = switch (game.status()) {
                case BLACK_WON -> "Black wins";
                case WHITE_WON -> "White wins";
                case DRAW -> "Draw";
                case IN_PROGRESS -> "";
            };
            status.setText(context + " | " + result);
        }
        updatePlayerCards(game);
        configureToolbarButton(pauseButton, paused ? "Resume" : "Pause",
                paused ? ToolbarGlyph.PLAY : ToolbarGlyph.PAUSE);
        configureToolbarButton(passDrawButton,
                drawOfferBy == null ? "Pass to opponent" : "Hand to " + drawOfferBy.opposite(),
                ToolbarGlyph.HANDOFF);
        pauseButton.setEnabled(game.status() == GameStatus.IN_PROGRESS);
        undoButton.setEnabled(aiWorker == null
                && UndoPolicy.movesToUndo(
                        setup.configuration().mode(),
                        undoSnapshots.size(),
                        setup.configuration().firstPlayerSide()) > 0);
        boolean drawResponseVisible = GameWindowPolicy.showDrawResponseButtons(
                setup.configuration().mode(),
                game.status(),
                drawOfferBy != null,
                drawResponseReady);
        boolean drawOfferPending = setup.configuration().mode() == GameMode.PLAYER_VS_PLAYER
                && game.status() == GameStatus.IN_PROGRESS
                && drawOfferBy != null;
        offerDrawButton.setEnabled(game.status() == GameStatus.IN_PROGRESS
                && setup.configuration().mode() == GameMode.PLAYER_VS_PLAYER
                && drawOfferBy == null);
        passDrawButton.setVisible(drawOfferPending && !drawResponseReady);
        passDrawButton.setEnabled(drawOfferPending && !drawResponseReady);
        agreeDrawButton.setVisible(drawResponseVisible);
        declineDrawButton.setVisible(drawResponseVisible);
        agreeDrawButton.setEnabled(drawResponseVisible);
        declineDrawButton.setEnabled(drawResponseVisible);
        boardPanel.repaint();
    }

    private void updatePlayerCards(GameState game) {
        updatePlayerCard(blackPlayerCard, Side.BLACK, game);
        updatePlayerCard(whitePlayerCard, Side.WHITE, game);
    }

    private void updatePlayerCard(GamePlayerCard card, Side side, GameState game) {
        boolean active = game.status() == GameStatus.IN_PROGRESS && game.currentPlayer() == side;
        String turnStatus;
        if (game.status() == GameStatus.DRAW) {
            turnStatus = "DRAW";
        } else if (game.status() == GameStatus.BLACK_WON || game.status() == GameStatus.WHITE_WON) {
            turnStatus = (game.status() == GameStatus.BLACK_WON && side == Side.BLACK)
                            || (game.status() == GameStatus.WHITE_WON && side == Side.WHITE)
                    ? "WINNER"
                    : "GAME OVER";
        } else if (!active) {
            turnStatus = "WAITING";
        } else if (paused) {
            turnStatus = "PAUSED";
        } else if (aiWorker != null && !setup.controllerFor(side).isHuman()) {
            turnStatus = "THINKING";
        } else {
            turnStatus = "TO MOVE";
        }
        card.update(setup.controllerFor(side), active, turnStatus);
    }

    private void offerDraw() {
        if (setup.game().status() != GameStatus.IN_PROGRESS || drawOfferBy != null) {
            return;
        }
        drawOfferBy = setup.game().currentPlayer();
        drawResponseReady = false;
        refresh();
        status.setText(drawOfferBy + " offers a draw. Hand the game to " + drawOfferBy.opposite() + ".");
    }

    private void passDrawToOpponent() {
        if (drawOfferBy == null || !passDrawButton.isVisible()) {
            return;
        }
        drawResponseReady = true;
        refresh();
        status.setText(drawOfferBy.opposite() + " can agree to or decline " + drawOfferBy + "'s draw offer.");
    }

    private void agreeToDraw() {
        if (!GameWindowPolicy.showDrawResponseButtons(
                setup.configuration().mode(),
                setup.game().status(),
                drawOfferBy != null,
                drawResponseReady)) {
            return;
        }
        setup.game().agreeToDraw();
        drawOfferBy = null;
        drawResponseReady = false;
        refresh();
    }

    private void declineDraw() {
        if (GameWindowPolicy.showDrawResponseButtons(
                setup.configuration().mode(),
                setup.game().status(),
                drawOfferBy != null,
                drawResponseReady)) {
            drawOfferBy = null;
            drawResponseReady = false;
        }
        refresh();
    }

    private void makeMove(Move move) {
        GameState game = setup.game();
        Piece movingPiece = game.pieceAt(move.from());
        undoSnapshots.add(game.copy());
        game.makeMove(move);
        boardPanel.animateMove(move, movingPiece);
        drawOfferBy = null;
        drawResponseReady = false;
    }

    private void undo() {
        GameMode mode = setup.configuration().mode();
        int moveCount = UndoPolicy.movesToUndo(mode, undoSnapshots.size(),
                setup.configuration().firstPlayerSide());
        if (moveCount == 0 || aiWorker != null) {
            return;
        }
        GameState snapshot = undoSnapshots.get(undoSnapshots.size() - moveCount);
        setup.game().restoreFrom(snapshot);
        undoSnapshots.subList(undoSnapshots.size() - moveCount, undoSnapshots.size()).clear();
        drawOfferBy = null;
        clearSelection();
        List<Move> history = setup.game().history();
        boardPanel.setLastMove(history.isEmpty() ? null : history.get(history.size() - 1));
        refresh();
        startAiTurn();
    }

    private void configureToolbarButton(JButton button, String text, ToolbarGlyph glyph) {
        button.setText(text);
        button.setIcon(new ToolbarIcon(glyph));
        button.setToolTipText(text);
        button.getAccessibleContext().setAccessibleName(text);
        button.setIconTextGap(6);
        button.setFocusPainted(false);
        button.setMargin(new Insets(6, 10, 6, 10));
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void returnToMenu() {
        if (returning) {
            return;
        }
        returning = true;
        aiTurnGeneration++;
        if (aiWorker != null) {
            aiWorker.cancel(true);
        }
        GameConfiguration nextConfiguration = setup.game().status() == GameStatus.IN_PROGRESS
                ? setup.configuration()
                : setup.configuration().nextGame();
        dispose();
        returnHandler.accept(nextConfiguration);
    }

    private enum ToolbarGlyph {
        PAUSE,
        PLAY,
        UNDO,
        OFFER,
        HANDOFF,
        ACCEPT,
        DECLINE,
        MENU
    }

    private static final class ToolbarIcon implements Icon {
        private final ToolbarGlyph glyph;

        private ToolbarIcon(ToolbarGlyph glyph) {
            this.glyph = glyph;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(component.isEnabled() ? new Color(58, 70, 83) : new Color(155, 160, 166));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case PAUSE -> {
                    g.fillRoundRect(x + 4, y + 3, 4, 12, 2, 2);
                    g.fillRoundRect(x + 11, y + 3, 4, 12, 2, 2);
                }
                case PLAY -> g.fillPolygon(new Polygon(
                        new int[] {x + 5, x + 15, x + 5},
                        new int[] {y + 3, y + 9, y + 15},
                        3));
                case UNDO -> {
                    g.drawArc(x + 4, y + 4, 11, 11, 35, 285);
                    g.drawLine(x + 4, y + 4, x + 4, y + 9);
                    g.drawLine(x + 4, y + 4, x + 9, y + 4);
                }
                case OFFER -> {
                    g.drawRoundRect(x + 2, y + 3, 14, 10, 3, 3);
                    g.drawLine(x + 5, y + 13, x + 3, y + 16);
                    g.drawLine(x + 5, y + 13, x + 8, y + 13);
                }
                case HANDOFF -> {
                    g.drawLine(x + 3, y + 6, x + 14, y + 6);
                    g.drawLine(x + 11, y + 3, x + 14, y + 6);
                    g.drawLine(x + 11, y + 9, x + 14, y + 6);
                    g.drawLine(x + 15, y + 12, x + 4, y + 12);
                    g.drawLine(x + 7, y + 9, x + 4, y + 12);
                    g.drawLine(x + 7, y + 15, x + 4, y + 12);
                }
                case ACCEPT -> {
                    g.drawLine(x + 3, y + 9, x + 7, y + 13);
                    g.drawLine(x + 7, y + 13, x + 15, y + 4);
                }
                case DECLINE -> {
                    g.drawLine(x + 4, y + 4, x + 14, y + 14);
                    g.drawLine(x + 14, y + 4, x + 4, y + 14);
                }
                case MENU -> {
                    g.drawLine(x + 3, y + 4, x + 15, y + 4);
                    g.drawLine(x + 3, y + 9, x + 15, y + 9);
                    g.drawLine(x + 3, y + 14, x + 15, y + 14);
                }
            }
            g.dispose();
        }
    }
}
