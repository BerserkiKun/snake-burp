package burp.engine;

import burp.model.Difficulty;
import burp.model.Direction;
import burp.model.Food;
import burp.model.GameState;
import burp.model.Point;
import burp.model.Snake;

import java.util.Random;

/**
 * Pure game logic — no Swing dependencies.
 * All state mutation occurs on the EDT via javax.swing.Timer callbacks.
 */
public class GameEngine {

    public static final int COLS = 40;
    public static final int ROWS = 25;

    private static final int SPEED_SCALE_EVERY = 5;
    private static final int SPEED_STEP_MS     = 10;
    private static final int MIN_INTERVAL_MS   = 40;

    private Snake snake;
    private Food  food;
    private GameState  state;
    private int score;
    private int highScore;
    private int foodEaten;
    private boolean    wrapMode;
    private Difficulty difficulty;

    private final Random random = new Random();
    private GameEventListener listener;

    public GameEngine() {
        difficulty = Difficulty.MEDIUM;
        state      = GameState.WAITING;
    }

    public void setListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void startNewGame() {
        Point start = new Point(COLS / 2, ROWS / 2);
        snake     = new Snake(start, Direction.RIGHT);
        food      = new Food(random);
        score     = 0;
        foodEaten = 0;
        food.respawn(COLS, ROWS, snake);
        state = GameState.RUNNING;
        notifyListener();
    }

    /**
     * Advances the game one tick. Must be called on EDT.
     * @return ms delay for the next tick
     */
    public int tick() {
        if (state != GameState.RUNNING) {
            return getCurrentInterval();
        }

        // Flush pending direction (moveTo will finalize it, but we need it now to compute nextHead)
        // We ask Snake to flush its pending direction so we read the correct upcoming direction.
        snake.flushPendingDirection();
        Direction dir = snake.getCurrentDirection();

        // Compute next head
        Point head = snake.getHead();
        int nx = head.x + dir.dx;
        int ny = head.y + dir.dy;

        if (wrapMode) {
            nx = Math.floorMod(nx, COLS);
            ny = Math.floorMod(ny, ROWS);
        } else if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) {
            endGame();
            return getCurrentInterval();
        }

        Point nextHead = new Point(nx, ny);
        boolean ate = nextHead.equals(food.getPosition());

        // Move snake
        snake.moveTo(nextHead, ate);

        // Self-collision
        if (snake.hasHeadCollidedWithBody()) {
            endGame();
            return getCurrentInterval();
        }

        if (ate) {
            score += computeScoreGain();
            foodEaten++;
            if (score > highScore) highScore = score;
            food.respawn(COLS, ROWS, snake);
        }

        notifyListener();
        return getCurrentInterval();
    }

    public void togglePause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
        notifyListener();
    }

    public void setDesiredDirection(Direction direction) {
        if (snake != null && state == GameState.RUNNING) {
            snake.setDesiredDirection(direction);
        }
    }

    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public void setWrapMode(boolean w)       { this.wrapMode = w; }

    private void endGame() {
        state = GameState.GAME_OVER;
        notifyListener();
    }

    private int computeScoreGain() {
        return switch (difficulty) {
            case EASY   -> 10;
            case MEDIUM -> 20;
            case HARD   -> 30;
        };
    }

    public int getCurrentInterval() {
        int base     = difficulty.getBaseIntervalMs();
        int steps    = foodEaten / SPEED_SCALE_EVERY;
        int interval = base - (steps * SPEED_STEP_MS);
        return Math.max(interval, MIN_INTERVAL_MS);
    }

    private void notifyListener() {
        if (listener != null) listener.onGameStateChanged(this);
    }

    public Snake      getSnake()      { return snake; }
    public Food       getFood()       { return food; }
    public GameState  getState()      { return state; }
    public int        getScore()      { return score; }
    public int        getHighScore()  { return highScore; }
    public boolean    isWrapMode()    { return wrapMode; }
    public Difficulty getDifficulty() { return difficulty; }
    public int        getFoodEaten()  { return foodEaten; }

    @FunctionalInterface
    public interface GameEventListener {
        void onGameStateChanged(GameEngine engine);
    }
}
