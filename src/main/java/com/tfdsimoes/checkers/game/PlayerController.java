package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.model.Move;

public interface PlayerController {
    boolean isHuman();

    String displayName();

    Move chooseMove(GameState game);
}
