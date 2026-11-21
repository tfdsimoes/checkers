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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

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
    private final DefaultListModel<String> moveHistoryModel = new DefaultListModel<>();
    private final JList<String> moveHistory = new JList<>(moveHistoryModel);
    private final GameBoardPanel boardPanel;
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

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                returnToMenu();
            }
        });
        setLayout(new BorderLayout(8, 8));
        add(status, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        moveHistory.setVisibleRowCount(20);
        moveHistory.setFixedCellWidth(120);
        JScrollPane historyScroll = new JScrollPane(moveHistory);
        historyScroll.setPreferredSize(new Dimension(150, 0));
        add(historyScroll, BorderLayout.EAST);

        JButton menuButton = new JButton("Menu");
        menuButton.addActionListener(event -> returnToMenu());
        pauseButton.addActionListener(event -> togglePause());
        undoButton.addActionListener(event -> undo());
        offerDrawButton.addActionListener(event -> offerDraw());
        passDrawButton.addActionListener(event -> passDrawToOpponent());
        agreeDrawButton.addActionListener(event -> agreeToDraw());
        declineDrawButton.addActionListener(event -> declineDraw());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
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
        pauseButton.setText(paused ? "Resume" : "Pause");
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
                    status.setText("AI move was cancelled.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("AI move was interrupted.");
                } catch (ExecutionException exception) {
                    status.setText("AI failed: " + exception.getCause().getMessage());
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    status.setText("AI returned an invalid move: " + exception.getMessage());
                }
            }
        };
        refresh();
        aiWorker.execute();
    }

    private void refresh() {
        GameState game = setup.game();
        refreshMoveHistory(game);
        String context = setup.configuration().mode() + " | seed " + setup.configuration().seed();
        if (game.status() == GameStatus.IN_PROGRESS) {
            Side side = game.currentPlayer();
            String player = setup.controllerFor(side).displayName();
            String turn = paused ? "Paused" : side + " to move (" + player + ")";
            String lastMove = game.history().isEmpty()
                    ? ""
                    : " | Last move: " + game.history().get(game.history().size() - 1).notation();
            status.setText(context + " | " + turn + lastMove + algorithmSuffix());
        } else {
            String result = switch (game.status()) {
                case BLACK_WON -> "Black wins";
                case WHITE_WON -> "White wins";
                case DRAW -> "Draw";
                case IN_PROGRESS -> "";
            };
            status.setText(context + " | " + result + algorithmSuffix());
        }
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
        passDrawButton.setText(drawOfferBy == null ? "Pass to opponent" : "Hand to " + drawOfferBy.opposite());
        passDrawButton.setVisible(drawOfferPending && !drawResponseReady);
        passDrawButton.setEnabled(drawOfferPending && !drawResponseReady);
        agreeDrawButton.setVisible(drawResponseVisible);
        declineDrawButton.setVisible(drawResponseVisible);
        agreeDrawButton.setEnabled(drawResponseVisible);
        declineDrawButton.setEnabled(drawResponseVisible);
        boardPanel.repaint();
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
        undoSnapshots.add(game.copy());
        game.makeMove(move);
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
        refresh();
        startAiTurn();
    }

    private String algorithmSuffix() {
        String names = setup.algorithmNames();
        return names.isEmpty() ? "" : " | " + names;
    }

    private void refreshMoveHistory(GameState game) {
        moveHistoryModel.clear();
        List<Move> history = game.history();
        for (int index = 0; index < history.size(); index++) {
            moveHistoryModel.addElement((index + 1) + ". " + history.get(index).notation());
        }
        if (!history.isEmpty()) {
            moveHistory.setSelectedIndex(history.size() - 1);
        }
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
}
