package com.tfdsimoes.checkers.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.tfdsimoes.checkers.game.GameConfiguration;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class CheckersApplication {
    private final MainMenuFrame menu;

    public CheckersApplication() {
        installLookAndFeel();
        menu = new MainMenuFrame(this::startGame);
    }

    public void show() {
        menu.setVisible(true);
    }

    private void startGame(GameConfiguration configuration) {
        menu.setVisible(false);
        GameSetupWindow gameWindow = new GameSetupWindow(configuration, this::returnToMenu);
        gameWindow.setVisible(true);
        gameWindow.start();
    }

    private void returnToMenu(GameConfiguration configuration) {
        menu.setConfiguration(configuration);
        menu.setVisible(true);
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException exception) {
            throw new IllegalStateException("Could not install FlatLaf", exception);
        }
    }
}
