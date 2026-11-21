package com.tfdsimoes.checkers.game;

import com.tfdsimoes.checkers.model.Move;
import java.util.Objects;

public final class HumanController implements PlayerController {
    private final String name;

    public HumanController(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public boolean isHuman() {
        return true;
    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public Move chooseMove(GameState game) {
        throw new UnsupportedOperationException("Human moves are entered through the user interface");
    }
}
