package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.game.GameConfiguration;
import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

final class MainMenuFrame extends JFrame {
    private final JComboBox<GameMode> mode = new JComboBox<>(GameMode.values());
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JComboBox<Side> firstPlayerSide = new JComboBox<>(Side.values());
    private final JTextField seed = new JTextField();
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private GameConfiguration returnedConfiguration;

    MainMenuFrame(Consumer<GameConfiguration> startGame) {
        super("Checkers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        add(new JLabel("Checkers", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Game mode"));
        fields.add(mode);
        fields.add(new JLabel("Difficulty"));
        fields.add(difficulty);
        fields.add(new JLabel("Your colour (PVM)"));
        fields.add(firstPlayerSide);
        fields.add(new JLabel("Random seed (optional)"));
        fields.add(seed);
        add(fields, BorderLayout.CENTER);

        JButton start = new JButton("Start game");
        start.addActionListener(event -> start(startGame));
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(message, BorderLayout.NORTH);
        footer.add(start, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        mode.addActionListener(event -> updateFields());
        updateFields();
        setSize(430, 280);
        setLocationRelativeTo(null);
    }

    void setConfiguration(GameConfiguration configuration) {
        returnedConfiguration = configuration;
        mode.setSelectedItem(configuration.mode());
        difficulty.setSelectedItem(configuration.difficulty());
        firstPlayerSide.setSelectedItem(configuration.firstPlayerSide());
        seed.setText(configuration.randomSeed() ? "" : Long.toString(configuration.seed()));
        message.setText(" ");
        updateFields();
    }

    private void start(Consumer<GameConfiguration> startGame) {
        try {
            GameMode selectedMode = (GameMode) mode.getSelectedItem();
            Difficulty selectedDifficulty = (Difficulty) difficulty.getSelectedItem();
            Side selectedSide = (Side) firstPlayerSide.getSelectedItem();
            GameConfiguration configuration = returnedConfiguration != null
                    && matchesCurrentSelection(returnedConfiguration, selectedMode, selectedDifficulty, selectedSide)
                    ? returnedConfiguration
                    : configurationFromSelection(selectedMode, selectedDifficulty, selectedSide);
            returnedConfiguration = null;
            seed.setText(configuration.randomSeed() ? "" : Long.toString(configuration.seed()));
            message.setText(" ");
            startGame.accept(configuration);
        } catch (NumberFormatException exception) {
            message.setText("Seed must be a whole number.");
        }
    }

    private boolean matchesCurrentSelection(
            GameConfiguration configuration,
            GameMode selectedMode,
            Difficulty selectedDifficulty,
            Side selectedSide) {
        String enteredSeed = seed.getText().trim();
        return configuration.mode() == selectedMode
                && configuration.difficulty() == selectedDifficulty
                && configuration.firstPlayerSide() == selectedSide
                && (configuration.randomSeed()
                        ? enteredSeed.isEmpty()
                        : Long.toString(configuration.seed()).equals(enteredSeed));
    }

    private GameConfiguration configurationFromSelection(
            GameMode selectedMode, Difficulty selectedDifficulty, Side selectedSide) {
        boolean randomSeed = seed.getText().isBlank();
        long seedValue = randomSeed
                ? new SplittableRandom().nextLong()
                : Long.parseLong(seed.getText().trim());
        return randomSeed
                ? GameConfiguration.withRandomSeed(selectedMode, selectedDifficulty, selectedSide, seedValue)
                : new GameConfiguration(selectedMode, selectedDifficulty, selectedSide, seedValue);
    }

    private void updateFields() {
        GameMode selectedMode = (GameMode) mode.getSelectedItem();
        boolean playerVsMachine = selectedMode == GameMode.PLAYER_VS_MACHINE;
        difficulty.setEnabled(playerVsMachine);
        firstPlayerSide.setEnabled(selectedMode != GameMode.MACHINE_VS_MACHINE);
    }
}
