package burp.ui;

import burp.ITab;
import burp.engine.GameEngine;
import burp.input.InputHandler;
import burp.model.Difficulty;
import burp.model.GameState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.Box;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Burp Suite tab container. Implements ITab and owns the game lifecycle.
 */
public class SnakeTab implements ITab {

    // ---------------------------------------------------------------
    // Core components
    // ---------------------------------------------------------------
    private final JPanel         rootPanel;
    private final SnakeGamePanel gamePanel;
    private final GameEngine     engine;
    private final Timer          gameTimer;

    // ---------------------------------------------------------------
    // UI controls (accessed on EDT only)
    // ---------------------------------------------------------------
    private final JLabel    scoreLabel;
    private final JLabel    highScoreLabel;
    private final JLabel    speedLabel;
    private final JButton   startRestartBtn;
    private final JButton   pauseBtn;
    private final JComboBox<Difficulty> difficultyBox;
    private final JCheckBox wrapCheckBox;

    public SnakeTab() {
        engine    = new GameEngine();
        gamePanel = new SnakeGamePanel(engine);

        // ---- Score / info labels ----
        scoreLabel     = makeLabel("Score: 0");
        highScoreLabel = makeLabel("Best: 0");
        speedLabel     = makeLabel("Speed: 1");

        // ---- Buttons ----
        startRestartBtn = new JButton("▶  Start");
        pauseBtn        = new JButton("⏸  Pause");
        pauseBtn.setEnabled(false);

        // ---- Difficulty selector ----
        difficultyBox = new JComboBox<>(Difficulty.values());
        difficultyBox.setSelectedItem(Difficulty.MEDIUM);

        // ---- Wrap mode toggle ----
        wrapCheckBox = new JCheckBox("Wrap Mode");
        wrapCheckBox.setOpaque(false);
        wrapCheckBox.setForeground(new Color(200, 200, 200));

        // ---- Top bar ----
        JPanel topBar = buildTopBar();

        // ---- Root layout ----
        rootPanel = new JPanel(new BorderLayout(0, 4));
        rootPanel.setBackground(new Color(45, 45, 45));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        rootPanel.add(topBar, BorderLayout.NORTH);
        rootPanel.add(gamePanel, BorderLayout.CENTER);

        // Wrap game panel in a centered panel
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(45, 45, 45));
        centerWrapper.add(gamePanel);
        rootPanel.add(centerWrapper, BorderLayout.CENTER);

        // ---- Game timer (does not start yet) ----
        gameTimer = new Timer(engine.getCurrentInterval(), e -> gameTick());
        gameTimer.setInitialDelay(0);

        // ---- Wire listeners ----
        wireListeners();

        // ---- Engine event callback ----
        engine.setListener(eng -> SwingUtilities.invokeLater(this::syncUi));
    }

    // ---------------------------------------------------------------
    // ITab contract
    // ---------------------------------------------------------------

    @Override
    public String getTabCaption() {
        return "Snake 🐍";
    }

    @Override
    public Component getUiComponent() {
        return rootPanel;
    }

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    /** Called when the Burp extension is unloaded. Stops the timer cleanly. */
    public void dispose() {
        gameTimer.stop();
    }

    // ---------------------------------------------------------------
    // Game loop
    // ---------------------------------------------------------------

    private void gameTick() {
        int nextDelay = engine.tick();
        if (gameTimer.getDelay() != nextDelay) {
            gameTimer.setDelay(nextDelay);
        }
        gamePanel.repaint();
    }

    // ---------------------------------------------------------------
    // UI wiring
    // ---------------------------------------------------------------

    private void wireListeners() {
        startRestartBtn.addActionListener(e -> startOrRestart());

        pauseBtn.addActionListener(e -> {
            engine.togglePause();
            gamePanel.requestFocusInWindow();
        });

        difficultyBox.addActionListener(e -> {
            Difficulty selected = (Difficulty) difficultyBox.getSelectedItem();
            if (selected != null) {
                engine.setDifficulty(selected);
            }
        });

        wrapCheckBox.addActionListener(e -> engine.setWrapMode(wrapCheckBox.isSelected()));

        // Input handler on game panel
        InputHandler inputHandler = new InputHandler(
            engine,
            this::startOrRestart,
            () -> {
                engine.togglePause();
                gamePanel.requestFocusInWindow();
            }
        );
        gamePanel.addKeyListener(inputHandler);
    }

    private void startOrRestart() {
        gameTimer.stop();
        engine.startNewGame();
        gameTimer.setDelay(engine.getCurrentInterval());
        gameTimer.start();
        syncUi();
        gamePanel.requestFocusInWindow();
    }

    // ---------------------------------------------------------------
    // UI sync (EDT only)
    // ---------------------------------------------------------------

    private void syncUi() {
        GameState state = engine.getState();
        scoreLabel.setText("Score: " + engine.getScore());
        highScoreLabel.setText("Best: "  + engine.getHighScore());
        int speedLevel = engine.getFoodEaten() / 5 + 1;
        speedLabel.setText("Speed: " + speedLevel);

        switch (state) {
            case WAITING -> {
                startRestartBtn.setText("▶  Start");
                pauseBtn.setEnabled(false);
            }
            case RUNNING -> {
                startRestartBtn.setText("⟳  Restart");
                pauseBtn.setEnabled(true);
                pauseBtn.setText("⏸  Pause");
            }
            case PAUSED -> {
                startRestartBtn.setText("⟳  Restart");
                pauseBtn.setEnabled(true);
                pauseBtn.setText("▶  Resume");
            }
            case GAME_OVER -> {
                gameTimer.stop();
                startRestartBtn.setText("⟳  Restart");
                pauseBtn.setEnabled(false);
            }
        }
        gamePanel.repaint();
    }

    // ---------------------------------------------------------------
    // Builders
    // ---------------------------------------------------------------

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bar.setBackground(new Color(55, 55, 55));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(80, 80, 80)),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));

        styleButton(startRestartBtn, new Color(70, 180, 100));
        styleButton(pauseBtn,        new Color(100, 140, 200));

        bar.add(startRestartBtn);
        bar.add(pauseBtn);
        bar.add(makeSeparator());
        bar.add(scoreLabel);
        bar.add(highScoreLabel);
        bar.add(speedLabel);
        bar.add(makeSeparator());
        JLabel diffLabel = new JLabel("Difficulty:");
        diffLabel.setForeground(new Color(180, 180, 180));
        bar.add(diffLabel);
        bar.add(difficultyBox);
        bar.add(wrapCheckBox);

        // Add a glue component to push the next component to the right
        bar.add(Box.createHorizontalGlue());
        
        // Create and add the Support Development button
        JButton supportBtn = new JButton("Support Development");
        styleButton(supportBtn, new Color(100, 100, 180)); // Purple-ish color
        // Add action listener to open URL in default browser
        supportBtn.addActionListener(e -> {
            String url = "https://github.com/berserkikun/snake-burp?tab=readme-ov-file#support-development";
            openUrlInBrowser(url);
        });
        bar.add(supportBtn);

        return bar;
    }

    private void openUrlInBrowser(String url) {
        try {
            // Check if Desktop is supported and browse action is supported
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                // Execute in a separate thread to avoid blocking EDT
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (IOException | URISyntaxException ex) {
                        // Log error and show user-friendly message
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(
                                rootPanel,
                                "Could not open browser. Please visit manually:\n" + url,
                                "Browser Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        );
                    }
                }).start();
            } else {
                // Fallback for systems where Desktop is not supported
                JOptionPane.showMessageDialog(
                    rootPanel,
                    "Desktop browsing not supported. Please visit:\n" + url,
                    "Browser Unavailable",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 13));
        lbl.setForeground(new Color(200, 200, 200));
        return lbl;
    }

    private static JPanel makeSeparator() {
        JPanel sep = new JPanel();
        sep.setPreferredSize(new java.awt.Dimension(1, 20));
        sep.setBackground(new Color(90, 90, 90));
        return sep;
    }

    private static void styleButton(JButton btn, Color accent) {
        btn.setBackground(accent);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setOpaque(true);
    }
}
