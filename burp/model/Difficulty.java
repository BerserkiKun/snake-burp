package burp.model;

public enum Difficulty {
    EASY("Easy", 200),
    MEDIUM("Medium", 130),
    HARD("Hard", 75);

    private final String label;
    private final int baseIntervalMs;

    Difficulty(String label, int baseIntervalMs) {
        this.label = label;
        this.baseIntervalMs = baseIntervalMs;
    }

    public int getBaseIntervalMs() {
        return baseIntervalMs;
    }

    @Override
    public String toString() {
        return label;
    }
}
