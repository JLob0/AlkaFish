package com.alkacode.fish.model;

import java.util.Random;

/** Estado de um torneio em andamento. */
public class Tournament {
    private final TournamentType type;
    private final int totalSeconds;
    private int remainingSeconds;
    private int targetRarity;

    public Tournament(TournamentType type, int totalSeconds) {
        this.type = type;
        this.totalSeconds = totalSeconds;
        this.remainingSeconds = totalSeconds;
        if (type == TournamentType.RANDOM_RARITY_FIRST) {
            this.targetRarity = 2 + new Random().nextInt(3); // Rare to Legendary
        }
    }

    public void tick() {
        remainingSeconds--;
    }

    public boolean isFinished() {
        return remainingSeconds <= 0;
    }

    public String getTimeLeftFormatted() {
        int mins = remainingSeconds / 60;
        int secs = remainingSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public TournamentType getType() { return type; }
    public int getTotalSeconds() { return totalSeconds; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public int getTargetRarity() { return targetRarity; }
}
