package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.model.Side;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

final class PieceTokenIcon implements Icon {
    private final Side side;

    PieceTokenIcon(Side side) {
        this.side = side;
    }

    @Override
    public int getIconWidth() {
        return 26;
    }

    @Override
    public int getIconHeight() {
        return 26;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = side == Side.BLACK ? new Color(47, 42, 37) : new Color(244, 238, 224);
        Color edge = side == Side.BLACK ? new Color(24, 21, 19) : new Color(133, 119, 97);
        g.setColor(fill);
        g.fillOval(x + 2, y + 3, 22, 20);
        g.setColor(edge);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x + 2, y + 3, 22, 20);
        g.setColor(side == Side.BLACK ? new Color(111, 101, 90) : Color.WHITE);
        g.drawArc(x + 5, y + 5, 16, 7, 10, 160);
        g.dispose();
    }
}
