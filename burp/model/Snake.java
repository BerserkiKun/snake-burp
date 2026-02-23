package burp.model;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the snake: an ordered deque of grid cells (head first).
 */
public class Snake {

    private final Deque<Point> body    = new ArrayDeque<>();
    private final Set<Point>  bodySet = new HashSet<>(); // O(1) containment check

    private Direction currentDirection;
    private Direction pendingDirection;

    public Snake(Point startPosition, Direction startDirection) {
        body.addFirst(startPosition);
        bodySet.add(startPosition);
        currentDirection = startDirection;
        pendingDirection = startDirection;
    }

    /** Queues a direction change; reverse direction is ignored. */
    public void setDesiredDirection(Direction desired) {
        if (!currentDirection.isOpposite(desired)) {
            pendingDirection = desired;
        }
    }

    /** Applies pendingDirection → currentDirection immediately (called by GameEngine before computing next head). */
    public void flushPendingDirection() {
        currentDirection = pendingDirection;
    }

    /**
     * Moves the snake to an explicitly computed next head position.
     * The caller (GameEngine) handles wrap/boundary before calling this.
     *
     * @param nextHead computed next head cell
     * @param grow     true when the snake ate food this tick
     */
    public void moveTo(Point nextHead, boolean grow) {
        // currentDirection already flushed by GameEngine via flushPendingDirection()
        body.addFirst(nextHead);
        bodySet.add(nextHead);

        if (!grow) {
            Point tail = body.removeLast();
            bodySet.remove(tail);
        }
    }

    /** Returns true if the head occupies a cell also occupied by any body segment. */
    public boolean hasHeadCollidedWithBody() {
        Point head = getHead();
        int hits = 0;
        for (Point p : body) {
            if (p.equals(head) && ++hits > 1) return true;
        }
        return false;
    }

    public Point getHead() {
        return body.peekFirst();
    }

    public boolean containsPoint(Point p) {
        return bodySet.contains(p);
    }

    public List<Point> getBodyAsList() {
        return Collections.unmodifiableList(body.stream().toList());
    }

    public int length() {
        return body.size();
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }
}
