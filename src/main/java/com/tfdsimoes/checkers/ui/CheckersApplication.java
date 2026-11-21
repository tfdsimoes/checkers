package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.game.GameConfiguration;

public final class CheckersApplication {
    private final MainMenuFrame menu;

    public CheckersApplication() {
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
}
