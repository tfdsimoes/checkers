package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameConfiguration;
import com.tfdsimoes.checkers.game.GameSetup;
import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.game.GameStatus;
import com.tfdsimoes.checkers.game.PlayerController;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Piece;
import com.tfdsimoes.checkers.model.Position;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

final class GameSetupWindow extends JFrame {
    private final GameSetup setup;
    private final Consumer<GameConfiguration> returnHandler;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JButton pauseButton = new JButton("Pause");
    private final GameBoardPanel boardPanel;
    private SwingWorker<Move, Void> aiWorker;
    private List<Move> selectedMoves = List.of();
    private Position selectedSource;
    private int selectedPathIndex;
    private boolean paused;
    private boolean returning;
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

        JButton menuButton = new JButton("Menu");
        menuButton.addActionListener(event -> returnToMenu());
        pauseButton.addActionListener(event -> togglePause());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controls.add(pauseButton);
        controls.add(menuButton);
        add(controls, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        refresh();
        startAiTurn();
    }

    private void handleBoardClick(Position position) {
        GameState game = setup.game();
        if (paused || game.status() != GameStatus.IN_PROGRESS
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
            game.makeMove(selectedMoves.get(0));
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
        if (!paused) {
            startAiTurn();
        }
        refresh();
    }

    private void startAiTurn() {
        GameState game = setup.game();
        if (returning || paused || game.status() != GameStatus.IN_PROGRESS || aiWorker != null
                || setup.controllerFor(game.currentPlayer()).isHuman()) {
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
                aiWorker = null;
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
                    game.makeMove(get());
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
        boardPanel.repaint();
    }

    private String algorithmSuffix() {
        String names = setup.algorithmNames();
        return names.isEmpty() ? "" : " | " + names;
    }

    private void returnToMenu() {
        if (returning) {
            return;
        }
        returning = true;
        aiTurnGeneration++;
        GameConfiguration nextConfiguration = setup.game().status() == GameStatus.IN_PROGRESS
                ? setup.configuration()
                : setup.configuration().nextGame();
        dispose();
        returnHandler.accept(nextConfiguration);
    }
}
