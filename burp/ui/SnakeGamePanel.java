package burp.ui;

import burp.engine.GameEngine;
import burp.model.Direction;
import burp.model.Food;
import burp.model.GameState;
import burp.model.Point;
import burp.model.Snake;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Renders the Snake game board. Pure rendering — no game logic.
 * Listens for mouse clicks to reclaim keyboard focus inside Burp.
 */
public class SnakeGamePanel extends JPanel {

    private static final int CELL_SIZE    = 24; // px per grid cell
    private static final int BORDER_WIDTH = 2;

    // Derived board pixel dimensions
    private static final int BOARD_WIDTH  = GameEngine.COLS * CELL_SIZE;
    private static final int BOARD_HEIGHT = GameEngine.ROWS * CELL_SIZE;

    // Color palette — compatible with both Burp dark and light themes
    private static final Color COLOR_BG           = new Color(30,  30,  30);
    private static final Color COLOR_GRID          = new Color(40,  40,  40);
    private static final Color COLOR_BORDER        = new Color(80, 200, 120);
    private static final Color COLOR_SNAKE_HEAD    = new Color(80, 220, 100);
    private static final Color COLOR_SNAKE_BODY    = new Color(50, 170,  70);
    private static final Color COLOR_SNAKE_OUTLINE = new Color(30, 120,  50);
    private static final Color COLOR_FOOD          = new Color(255,  80,  80);
    private static final Color COLOR_FOOD_SHINE    = new Color(255, 180, 180);
    private static final Color COLOR_TEXT_PRIMARY  = new Color(220, 220, 220);
    private static final Color COLOR_TEXT_DIM      = new Color(140, 140, 140);
    private static final Color COLOR_OVERLAY       = new Color(0, 0, 0, 160);
    private static final Color COLOR_PAUSED        = new Color(255, 200, 50);

    private static final Font FONT_OVERLAY = new Font("Monospaced", Font.BOLD, 28);
    private static final Font FONT_SUB     = new Font("Monospaced", Font.PLAIN, 14);

    private GameEngine engine;

    public SnakeGamePanel(GameEngine engine) {
        this.engine = engine;
        setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        setMinimumSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        
        // Center the panel within its parent
        setAlignmentX(CENTER_ALIGNMENT);
        setAlignmentY(CENTER_ALIGNMENT);
        
        setBackground(COLOR_BG);
        setFocusable(true);

        // Reclaim focus when user clicks the game board
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(BOARD_WIDTH, BOARD_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2);
        drawGrid(g2);
        drawBorder(g2);

        GameState state = engine.getState();

        if (state == GameState.WAITING) {
            drawWaitingOverlay(g2);
        } else {
            drawFood(g2);
            drawSnake(g2);

            if (state == GameState.PAUSED) {
                drawPausedOverlay(g2);
            } else if (state == GameState.GAME_OVER) {
                drawGameOverOverlay(g2);
            }
        }

        g2.dispose();
    }

    // ---------------------------------------------------------------
    // Drawing helpers
    // ---------------------------------------------------------------

    private void drawBackground(Graphics2D g2) {
        g2.setColor(COLOR_BG);
        g2.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(COLOR_GRID);
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x <= BOARD_WIDTH; x += CELL_SIZE) {
            g2.drawLine(x, 0, x, BOARD_HEIGHT);
        }
        for (int y = 0; y <= BOARD_HEIGHT; y += CELL_SIZE) {
            g2.drawLine(0, y, BOARD_WIDTH, y);
        }
    }

    private void drawBorder(Graphics2D g2) {
        g2.setColor(COLOR_BORDER);
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        g2.drawRect(BORDER_WIDTH / 2, BORDER_WIDTH / 2,
                    BOARD_WIDTH - BORDER_WIDTH, BOARD_HEIGHT - BORDER_WIDTH);
    }

    private void drawSnake(Graphics2D g2) {
        Snake snake = engine.getSnake();
        if (snake == null) return;

        List<Point> body = snake.getBodyAsList();
        for (int i = body.size() - 1; i >= 0; i--) {
            Point p = body.get(i);
            int px = p.x * CELL_SIZE;
            int py = p.y * CELL_SIZE;
            int pad = 2;

            if (i == 0) {
                // Head
                g2.setColor(COLOR_SNAKE_HEAD);
                g2.fillRoundRect(px + pad, py + pad,
                        CELL_SIZE - 2 * pad, CELL_SIZE - 2 * pad, 8, 8);
                g2.setColor(COLOR_SNAKE_OUTLINE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(px + pad, py + pad,
                        CELL_SIZE - 2 * pad, CELL_SIZE - 2 * pad, 8, 8);
                drawEyes(g2, p, snake.getCurrentDirection());
            } else {
                // Body — fade toward tail
                float ratio = (float) i / body.size();
                Color bodyColor = interpolateColor(COLOR_SNAKE_BODY, COLOR_BG, ratio * 0.35f);
                g2.setColor(bodyColor);
                g2.fillRoundRect(px + pad, py + pad,
                        CELL_SIZE - 2 * pad, CELL_SIZE - 2 * pad, 5, 5);
                g2.setColor(COLOR_SNAKE_OUTLINE);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(px + pad, py + pad,
                        CELL_SIZE - 2 * pad, CELL_SIZE - 2 * pad, 5, 5);
            }
        }
    }

    private void drawEyes(Graphics2D g2, Point head, Direction dir) {
        int px = head.x * CELL_SIZE;
        int py = head.y * CELL_SIZE;
        int half = CELL_SIZE / 2;
        int eyeSize = 4;
        int eyeOffset = 5;

        int ex1, ey1, ex2, ey2;
        switch (dir) {
            case UP -> {
                ex1 = px + eyeOffset;      ey1 = py + eyeOffset;
                ex2 = px + CELL_SIZE - eyeOffset - eyeSize; ey2 = ey1;
            }
            case DOWN -> {
                ex1 = px + eyeOffset;      ey1 = py + CELL_SIZE - eyeOffset - eyeSize;
                ex2 = px + CELL_SIZE - eyeOffset - eyeSize; ey2 = ey1;
            }
            case LEFT -> {
                ex1 = px + eyeOffset;      ey1 = py + eyeOffset;
                ex2 = ex1;                 ey2 = py + CELL_SIZE - eyeOffset - eyeSize;
            }
            default -> { // RIGHT
                ex1 = px + CELL_SIZE - eyeOffset - eyeSize; ey1 = py + eyeOffset;
                ex2 = ex1;                                   ey2 = py + CELL_SIZE - eyeOffset - eyeSize;
            }
        }
        g2.setColor(Color.BLACK);
        g2.fillOval(ex1, ey1, eyeSize, eyeSize);
        g2.fillOval(ex2, ey2, eyeSize, eyeSize);
        g2.setColor(Color.WHITE);
        g2.fillOval(ex1 + 1, ey1 + 1, eyeSize / 2, eyeSize / 2);
        g2.fillOval(ex2 + 1, ey2 + 1, eyeSize / 2, eyeSize / 2);
    }

    private void drawFood(Graphics2D g2) {
        Food food = engine.getFood();
        if (food == null || food.getPosition() == null) return;

        Point p  = food.getPosition();
        int px   = p.x * CELL_SIZE;
        int py   = p.y * CELL_SIZE;
        int pad  = 3;
        int size = CELL_SIZE - 2 * pad;

        // Glow effect
        g2.setColor(new Color(255, 80, 80, 60));
        g2.fillOval(px + pad - 2, py + pad - 2, size + 4, size + 4);

        // Food circle
        g2.setColor(COLOR_FOOD);
        g2.fillOval(px + pad, py + pad, size, size);

        // Shine
        g2.setColor(COLOR_FOOD_SHINE);
        g2.fillOval(px + pad + 2, py + pad + 2, size / 3, size / 3);
    }

    private void drawWaitingOverlay(Graphics2D g2) {
        drawDimOverlay(g2);
        g2.setColor(COLOR_TEXT_PRIMARY);
        g2.setFont(FONT_OVERLAY);
        drawCenteredString(g2, "SNAKE 🐍", BOARD_HEIGHT / 2 - 30);
        g2.setFont(FONT_SUB);
        g2.setColor(COLOR_TEXT_DIM);
        drawCenteredString(g2, "Press ENTER or click Start to play", BOARD_HEIGHT / 2 + 10);
        drawCenteredString(g2, "Arrow Keys / WASD to move  |  P to pause  |  R to restart", BOARD_HEIGHT / 2 + 32);
    }

    private void drawPausedOverlay(Graphics2D g2) {
        drawDimOverlay(g2);
        g2.setColor(COLOR_PAUSED);
        g2.setFont(FONT_OVERLAY);
        drawCenteredString(g2, "PAUSED", BOARD_HEIGHT / 2 - 14);
        g2.setFont(FONT_SUB);
        g2.setColor(COLOR_TEXT_DIM);
        drawCenteredString(g2, "Press P or ESC to resume", BOARD_HEIGHT / 2 + 18);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        drawDimOverlay(g2);
        g2.setColor(new Color(255, 80, 80));
        g2.setFont(FONT_OVERLAY);
        drawCenteredString(g2, "GAME OVER", BOARD_HEIGHT / 2 - 40);
        g2.setFont(FONT_SUB);
        g2.setColor(COLOR_TEXT_PRIMARY);
        drawCenteredString(g2, "Score: " + engine.getScore(), BOARD_HEIGHT / 2 - 4);
        drawCenteredString(g2, "High Score: " + engine.getHighScore(), BOARD_HEIGHT / 2 + 18);
        g2.setColor(COLOR_TEXT_DIM);
        drawCenteredString(g2, "Press ENTER or click Restart to play again", BOARD_HEIGHT / 2 + 42);
    }

    private void drawDimOverlay(Graphics2D g2) {
        g2.setColor(COLOR_OVERLAY);
        g2.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
    }

    private void drawCenteredString(Graphics2D g2, String text, int centerY) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (BOARD_WIDTH - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, centerY);
    }

    private Color interpolateColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed()   + t * (b.getRed()   - a.getRed()));
        int g = (int) (a.getGreen() + t * (b.getGreen() - a.getGreen()));
        int bv= (int) (a.getBlue()  + t * (b.getBlue()  - a.getBlue()));
        return new Color(r, g, bv);
    }

    public static int boardWidth()  { return BOARD_WIDTH; }
    public static int boardHeight() { return BOARD_HEIGHT; }
}
