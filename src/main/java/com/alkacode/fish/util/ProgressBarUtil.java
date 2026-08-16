package com.alkacode.fish.util;

/** Barra de progresso para lore/scoreboard. */
public final class ProgressBarUtil {

    private ProgressBarUtil() {}

    public static String create(double current, double max, int length, char symbol, String filledColor, String emptyColor) {
        if (max <= 0) return filledColor + String.valueOf(symbol).repeat(length);
        int filled = (int) Math.round((current / max) * length);
        filled = Math.max(0, Math.min(length, filled));
        int empty = length - filled;
        return filledColor + String.valueOf(symbol).repeat(filled) + emptyColor + String.valueOf(symbol).repeat(empty);
    }
}
