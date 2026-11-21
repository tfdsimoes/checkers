package com.tfdsimoes.checkers.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tfdsimoes.checkers.ai.AlgorithmRegistry;
import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.model.Side;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GameSetupTest {
    @Test
    void playerVersusPlayerUsesTwoHumanControllers() {
        GameSetup setup = GameSetup.create(configuration(GameMode.PLAYER_VS_PLAYER, Side.BLACK));

        assertTrue(setup.controllerFor(Side.BLACK).isHuman());
        assertTrue(setup.controllerFor(Side.WHITE).isHuman());
    }

    @Test
    void playerVersusMachineHonoursChosenHumanColour() {
        GameSetup setup = GameSetup.create(configuration(GameMode.PLAYER_VS_MACHINE, Side.WHITE));

        assertFalse(setup.controllerFor(Side.BLACK).isHuman());
        assertTrue(setup.controllerFor(Side.WHITE).isHuman());
        assertFalse(setup.controllerFor(Side.BLACK).displayName().isBlank());
    }

    @Test
    void machineVersusMachineSelectsTwoHardAlgorithms() {
        GameSetup setup = GameSetup.create(configuration(GameMode.MACHINE_VS_MACHINE, Side.BLACK));
        Set<String> hardAlgorithms = new AlgorithmRegistry().strategies(Difficulty.HARD).stream()
                .map(strategy -> strategy.name())
                .collect(Collectors.toSet());

        assertFalse(setup.controllerFor(Side.BLACK).isHuman());
        assertFalse(setup.controllerFor(Side.WHITE).isHuman());
        assertTrue(hardAlgorithms.contains(setup.controllerFor(Side.BLACK).displayName()));
        assertTrue(hardAlgorithms.contains(setup.controllerFor(Side.WHITE).displayName()));
    }

    @Test
    void gameSeedProducesRepeatableAlgorithmSelection() {
        GameConfiguration configuration = configuration(GameMode.PLAYER_VS_MACHINE, Side.BLACK);

        assertEquals(GameSetup.create(configuration).algorithmNames(),
                GameSetup.create(configuration).algorithmNames());
        assertEquals(Side.WHITE, configuration.nextGame().firstPlayerSide());
    }

    private static GameConfiguration configuration(GameMode mode, Side side) {
        return new GameConfiguration(mode, Difficulty.MEDIUM, side, 12345L);
    }
}
