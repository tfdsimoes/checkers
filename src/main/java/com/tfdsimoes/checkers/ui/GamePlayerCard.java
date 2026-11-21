package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.PlayerController;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

final class GamePlayerCard extends JPanel {
    private static final Color ACTIVE_BORDER = new Color(46, 112, 90);
    private static final Color ACTIVE_BACKGROUND = new Color(236, 247, 241);
    private static final Color INACTIVE_BORDER = new Color(200, 202, 207);
    private static final Color INACTIVE_BACKGROUND = new Color(250, 250, 251);

    private final Side side;
    private final JLabel sideName = new JLabel();
    private final JLabel turnIndicator = new JLabel("", SwingConstants.RIGHT);
    private final JLabel playerName = new JLabel();
    private final JLabel algorithm = new JLabel();

    GamePlayerCard(Side side) {
        super(new BorderLayout(8, 6));
        this.side = Objects.requireNonNull(side, "side");
        setPreferredSize(new Dimension(220, 92));

        sideName.setIcon(new PieceTokenIcon(side));
        sideName.setIconTextGap(8);
        sideName.setFont(sideName.getFont().deriveFont(Font.BOLD, 14f));
        turnIndicator.setFont(turnIndicator.getFont().deriveFont(Font.BOLD, 10f));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(sideName, BorderLayout.CENTER);
        header.add(turnIndicator, BorderLayout.EAST);

        JPanel details = new JPanel(new BorderLayout(8, 2));
        details.setOpaque(false);
        details.add(playerName, BorderLayout.NORTH);
        details.add(algorithm, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
        update(null, false, "WAITING");
    }

    void update(PlayerController controller, boolean active, String turnStatus) {
        CardDetails details = summarize(side, controller, active, turnStatus);
        sideName.setText(details.sideName());
        playerName.setText("Player: " + details.playerName());
        algorithm.setText("Algorithm: " + details.algorithm());
        turnIndicator.setText(details.turnStatus());
        Color border = details.active() ? ACTIVE_BORDER : INACTIVE_BORDER;
        setBackground(details.active() ? ACTIVE_BACKGROUND : INACTIVE_BACKGROUND);
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, details.active() ? 2 : 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    static CardDetails summarize(Side side, PlayerController controller, boolean active, String turnStatus) {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(turnStatus, "turnStatus");
        boolean human = controller == null || controller.isHuman();
        String playerName = controller == null
                ? "Waiting"
                : human ? controller.displayName() : "Machine";
        String algorithm = controller == null
                ? "Not assigned"
                : human ? "Human" : controller.displayName();
        return new CardDetails(side.toString(), playerName, algorithm, turnStatus, active);
    }

    record CardDetails(String sideName, String playerName, String algorithm, String turnStatus, boolean active) {}
}
