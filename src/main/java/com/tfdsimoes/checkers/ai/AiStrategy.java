package com.tfdsimoes.checkers.ai;

import com.tfdsimoes.checkers.game.GameState;
import com.tfdsimoes.checkers.model.Move;
import java.util.random.RandomGenerator;

public interface AiStrategy {
    String name();

    Move chooseMove(GameState game, RandomGenerator random);
}
