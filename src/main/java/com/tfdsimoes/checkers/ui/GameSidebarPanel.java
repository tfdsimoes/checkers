package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Board;
import com.tfdsimoes.checkers.model.Move;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

final class GameSidebarPanel extends JPanel {
    private final JLabel blackMaterial = new JLabel();
    private final JLabel whiteMaterial = new JLabel();
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> history = new JList<>(historyModel);

    GameSidebarPanel() {
        super(new BorderLayout(8, 8));
        setPreferredSize(new Dimension(190, 0));

        JPanel material = new JPanel(new GridLayout(0, 1, 4, 4));
        material.setBorder(BorderFactory.createTitledBorder("Material"));
        blackMaterial.setFont(blackMaterial.getFont().deriveFont(Font.BOLD));
        whiteMaterial.setFont(whiteMaterial.getFont().deriveFont(Font.BOLD));
        material.add(blackMaterial);
        material.add(whiteMaterial);
        add(material, BorderLayout.NORTH);

        history.setVisibleRowCount(20);
        JPanel moves = new JPanel(new BorderLayout());
        moves.setBorder(BorderFactory.createTitledBorder("Moves"));
        moves.add(new JScrollPane(history), BorderLayout.CENTER);
        add(moves, BorderLayout.CENTER);
    }

    void update(GameState game) {
        SidebarSummary summary = summarize(game);
        blackMaterial.setText("Black: " + summary.blackPieces());
        whiteMaterial.setText("White: " + summary.whitePieces());
        historyModel.clear();
        summary.moveHistory().forEach(historyModel::addElement);
        if (historyModel.getSize() > 0) {
            history.ensureIndexIsVisible(historyModel.getSize() - 1);
        }
    }

    static SidebarSummary summarize(GameState game) {
        Objects.requireNonNull(game, "game");
        Board board = game.board();
        List<Move> moves = game.history();
        List<String> moveHistory = new ArrayList<>(moves.size());
        for (int index = 0; index < moves.size(); index++) {
            moveHistory.add((index + 1) + ". " + moves.get(index).notation());
        }
        return new SidebarSummary(
                board.pieceCount(Side.BLACK),
                board.pieceCount(Side.WHITE),
                moveHistory);
    }

    record SidebarSummary(int blackPieces, int whitePieces, List<String> moveHistory) {
        SidebarSummary {
            moveHistory = List.copyOf(moveHistory);
        }
    }
}
