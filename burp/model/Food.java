package burp.model;

import java.util.Random;

public class Food {

    private Point position;
    private final Random random;

    public Food(Random random) {
        this.random = random;
    }

    /**
     * Places food at a random grid cell not occupied by the snake.
     */
    public void respawn(int cols, int rows, Snake snake) {
        Point candidate;
        int attempts = 0;
        do {
            int x = random.nextInt(cols);
            int y = random.nextInt(rows);
            candidate = new Point(x, y);
            attempts++;
            // Safety: avoid infinite loop if board is almost full
            if (attempts > cols * rows * 2) break;
        } while (snake.containsPoint(candidate));
        position = candidate;
    }

    public Point getPosition() {
        return position;
    }
}
