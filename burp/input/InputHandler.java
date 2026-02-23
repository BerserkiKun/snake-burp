package burp.input;

import burp.engine.GameEngine;
import burp.model.Direction;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Translates keyboard events into game commands.
 * Registered on the game panel component.
 */
public class InputHandler extends KeyAdapter {

    private final GameEngine engine;
    private final Runnable   onRestart;
    private final Runnable   onPause;

    public InputHandler(GameEngine engine, Runnable onRestart, Runnable onPause) {
        this.engine    = engine;
        this.onRestart = onRestart;
        this.onPause   = onPause;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP,    KeyEvent.VK_W -> engine.setDesiredDirection(Direction.UP);
            case KeyEvent.VK_DOWN,  KeyEvent.VK_S -> engine.setDesiredDirection(Direction.DOWN);
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A -> engine.setDesiredDirection(Direction.LEFT);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> engine.setDesiredDirection(Direction.RIGHT);
            case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> onPause.run();
            case KeyEvent.VK_R                     -> onRestart.run();
            case KeyEvent.VK_ENTER -> {
                if (engine.getState() != burp.model.GameState.RUNNING) {
                    onRestart.run();
                }
            }
        }
        e.consume();
    }
}
