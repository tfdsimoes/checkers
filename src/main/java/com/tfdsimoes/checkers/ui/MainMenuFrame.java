package com.tfdsimoes.checkers.ui;

import com.tfdsimoes.checkers.ai.Difficulty;
import com.tfdsimoes.checkers.game.GameConfiguration;
import com.tfdsimoes.checkers.game.GameMode;
import com.tfdsimoes.checkers.model.Side;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

final class MainMenuFrame extends JFrame {
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final ButtonGroup difficultyGroup = new ButtonGroup();
    private final ButtonGroup colourGroup = new ButtonGroup();
    private final Map<GameMode, JToggleButton> modeButtons = new EnumMap<>(GameMode.class);
    private final Map<Difficulty, JToggleButton> difficultyButtons = new EnumMap<>(Difficulty.class);
    private final Map<Side, JToggleButton> colourButtons = new EnumMap<>(Side.class);
    private final JTextField seed = new JTextField(16);
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private GameConfiguration returnedConfiguration;

    MainMenuFrame(Consumer<GameConfiguration> startGame) {
        super("Checkers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        content.add(createBanner(), BorderLayout.NORTH);
        content.add(createOptions(), BorderLayout.CENTER);
        content.add(createFooter(startGame), BorderLayout.SOUTH);
        setContentPane(content);

        modeButtons.get(GameMode.PLAYER_VS_PLAYER).setSelected(true);
        difficultyButtons.get(Difficulty.MEDIUM).setSelected(true);
        colourButtons.get(Side.BLACK).setSelected(true);
        updateFields();
        pack();
        setLocationRelativeTo(null);
    }

    void setConfiguration(GameConfiguration configuration) {
        returnedConfiguration = configuration;
        modeButtons.get(configuration.mode()).setSelected(true);
        difficultyButtons.get(configuration.difficulty()).setSelected(true);
        colourButtons.get(configuration.firstPlayerSide()).setSelected(true);
        seed.setText(configuration.randomSeed() ? "" : Long.toString(configuration.seed()));
        message.setText(" ");
        updateFields();
    }

    private JPanel createBanner() {
        JPanel banner = new JPanel(new BorderLayout(4, 4));
        JLabel title = new JLabel("CHECKERS", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        JLabel subtitle = new JLabel("Choose how you want to play", SwingConstants.CENTER);
        subtitle.setForeground(new Color(95, 96, 101));
        banner.add(title, BorderLayout.CENTER);
        banner.add(subtitle, BorderLayout.SOUTH);
        return banner;
    }

    private JPanel createOptions() {
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(createModeChoices());
        options.add(createDifficultyChoices());
        options.add(createColourChoices());
        options.add(createSeedInput());
        return options;
    }

    private JPanel createModeChoices() {
        JPanel choices = new JPanel(new GridLayout(1, 3, 8, 0));
        for (GameMode gameMode : GameMode.values()) {
            JToggleButton button = choiceButton(gameMode.toString(), null);
            button.addActionListener(event -> updateFields());
            modeButtons.put(gameMode, button);
            modeGroup.add(button);
            choices.add(button);
        }
        return section("Game mode", choices);
    }

    private JPanel createDifficultyChoices() {
        JPanel choices = new JPanel(new GridLayout(1, Difficulty.values().length, 8, 0));
        for (Difficulty value : Difficulty.values()) {
            JToggleButton button = choiceButton(value.toString(), null);
            difficultyButtons.put(value, button);
            difficultyGroup.add(button);
            choices.add(button);
        }
        return section("Machine difficulty", choices);
    }

    private JPanel createColourChoices() {
        JPanel choices = new JPanel(new GridLayout(1, Side.values().length, 8, 0));
        for (Side side : Side.values()) {
            String label = side == Side.BLACK ? "Black" : "White";
            JToggleButton button = choiceButton(label, new PieceChoiceIcon(side));
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
            button.setIconTextGap(10);
            colourButtons.put(side, button);
            colourGroup.add(button);
            choices.add(button);
        }
        return section("Human colour (Player vs Machine)", choices);
    }

    private JPanel createSeedInput() {
        JPanel seedInput = new JPanel(new BorderLayout(8, 0));
        seedInput.add(new JLabel("Random seed (blank = random)"), BorderLayout.WEST);
        seedInput.add(seed, BorderLayout.CENTER);
        JButton randomButton = new JButton("Random");
        randomButton.addActionListener(event -> {
            returnedConfiguration = null;
            seed.setText("");
            seed.requestFocusInWindow();
        });
        seedInput.add(randomButton, BorderLayout.EAST);
        return section("Replay and debugging", seedInput);
    }

    private JPanel createFooter(Consumer<GameConfiguration> startGame) {
        JButton start = new JButton("Start game");
        start.setFont(start.getFont().deriveFont(Font.BOLD, 15f));
        start.setPreferredSize(new Dimension(180, 42));
        start.addActionListener(event -> start(startGame));
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.add(message, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(start);
        footer.add(buttonPanel, BorderLayout.CENTER);
        return footer;
    }

    private JPanel section(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout(8, 8));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        section.add(content, BorderLayout.CENTER);
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }

    private JToggleButton choiceButton(String text, Icon icon) {
        JToggleButton button = new JToggleButton(text, icon);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(145, 42));
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    private void start(Consumer<GameConfiguration> startGame) {
        try {
            GameMode selectedMode = selectedValue(modeButtons);
            Difficulty selectedDifficulty = selectedValue(difficultyButtons);
            Side selectedSide = selectedValue(colourButtons);
            GameConfiguration configuration = returnedConfiguration != null
                            && matchesCurrentSelection(
                                    returnedConfiguration, selectedMode, selectedDifficulty, selectedSide)
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
        boolean playerVsMachine = selectedValue(modeButtons) == GameMode.PLAYER_VS_MACHINE;
        difficultyButtons.values().forEach(button -> button.setEnabled(playerVsMachine));
        boolean showColourChoices = selectedValue(modeButtons) == GameMode.PLAYER_VS_MACHINE;
        colourButtons.values().forEach(button -> button.setEnabled(showColourChoices));
    }

    private static <T extends Enum<T>> T selectedValue(Map<T, JToggleButton> choices) {
        return choices.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A menu choice must be selected"));
    }

    private static final class PieceChoiceIcon implements Icon {
        private final Side side;

        private PieceChoiceIcon(Side side) {
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
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
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
}
