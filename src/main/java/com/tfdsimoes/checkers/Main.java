package com.tfdsimoes.checkers;

import com.tfdsimoes.checkers.ui.CheckersApplication;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CheckersApplication().show());
    }
}
