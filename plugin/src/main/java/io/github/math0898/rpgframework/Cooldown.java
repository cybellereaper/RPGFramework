package io.github.math0898.rpgframework;

public final class Cooldown {

    private static final long MILLIS_PER_SECOND = 1000L;

    private final long durationSeconds;
    private long startTimeMillis;
    private boolean completedEarly;

    public Cooldown(float durationSeconds) {
        this((long) durationSeconds);
    }

    public Cooldown(long durationSeconds) {
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("Cooldown duration cannot be negative.");
        }

        this.durationSeconds = durationSeconds;
        this.startTimeMillis = System.currentTimeMillis();
        this.completedEarly = false;
    }

    public float getRemaining() {
        long elapsedSeconds = getElapsedSeconds();
        long remainingSeconds = durationSeconds - elapsedSeconds;
        return remainingSeconds;
    }

    public boolean isComplete() {
        return completedEarly || getRemaining() <= 0;
    }

    public void restart() {
        startTimeMillis = System.currentTimeMillis();
        completedEarly = false;
    }

    public void setComplete() {
        completedEarly = true;
    }

    private long getElapsedSeconds() {
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        return elapsedMillis / MILLIS_PER_SECOND;
    }
}
