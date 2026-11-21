package com.tfdsimoes.checkers.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void playerVersusMachineHonoursEveryDifficultyAndHumanColour() {
        AlgorithmRegistry registry = new AlgorithmRegistry();
        for (Difficulty difficulty : Difficulty.values()) {
            for (Side humanSide : Side.values()) {
                GameConfiguration configuration =
                        new GameConfiguration(GameMode.PLAYER_VS_MACHINE, difficulty, humanSide, 12345L);
                GameSetup setup = GameSetup.create(configuration);
                Side machineSide = humanSide.opposite();
                Set<String> poolNames = registry.strategies(difficulty).stream()
                        .map(strategy -> strategy.name())
                        .collect(Collectors.toSet());

                assertTrue(setup.controllerFor(humanSide).isHuman());
                assertFalse(setup.controllerFor(machineSide).isHuman());
                assertTrue(poolNames.contains(setup.controllerFor(machineSide).displayName()));
                assertEquals(Side.BLACK, setup.game().currentPlayer());
            }
        }
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
        assertEquals(12346L, configuration.nextGame().seed());
    }

    @Test
    void randomSeedChangesForRematches() {
        GameConfiguration configuration = GameConfiguration.withRandomSeed(
                GameMode.PLAYER_VS_MACHINE, Difficulty.MEDIUM, Side.WHITE, 12345L);

        GameConfiguration rematch = configuration.nextGame();

        assertTrue(rematch.randomSeed());
        assertNotEquals(configuration.seed(), rematch.seed());
        assertEquals(Side.BLACK, rematch.firstPlayerSide());
    }

    @Test
    void fixedSeedCanStartANewSessionAtTheOriginalValue() {
        GameConfiguration firstSession = configuration(GameMode.PLAYER_VS_MACHINE, Side.BLACK);
        GameConfiguration rematch = firstSession.nextGame();
        GameConfiguration newSession = configuration(GameMode.PLAYER_VS_MACHINE, Side.BLACK);

        assertEquals(12345L, firstSession.seed());
        assertEquals(12346L, rematch.seed());
        assertEquals(12345L, newSession.seed());
        assertFalse(newSession.randomSeed());
    }

    private static GameConfiguration configuration(GameMode mode, Side side) {
        return new GameConfiguration(mode, Difficulty.MEDIUM, side, 12345L);
    }
}
