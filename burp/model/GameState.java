package burp.model;

public enum GameState {
    WAITING,   // Before first start
    RUNNING,   // Active gameplay
    PAUSED,    // Player paused
    GAME_OVER  // Collision occurred
}
