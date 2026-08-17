package com.alkacode.fish.model;

/** Booster ativo de um jogador (FISH_CHANCE / NACAR_MULTIPLIER / SELL_BONUS). */
public class Booster {
    private final String type;
    private final double multiplier;
    private final long startTime;
    private final int durationSeconds;

    public Booster(String type, double multiplier, long startTime, int durationSeconds) {
        this.type = type;
        this.multiplier = multiplier;
        this.startTime = startTime;
        this.durationSeconds = durationSeconds;
    }

    public String getType() { return type; }
    public double getMultiplier() { return multiplier; }
    public long getStartTime() { return startTime; }
    public int getDurationSeconds() { return durationSeconds; }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > durationSeconds * 1000L;
    }

    public long getRemainingSeconds() {
        long remaining = (startTime + durationSeconds * 1000L - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
