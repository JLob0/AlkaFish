package com.alkacode.fish.api;

/**
 * API de boosts de pesca do AlkaFish - consumida por outros plugins (ex: Vips)
 * para expor multiplicadores ativos (sorte, XP). 1.0 = sem boost.
 */
public interface AlkaFishBoostAPI {

    /** Multiplicador de sorte na pesca atualmente ativo (1.0 = sem boost). */
    double getLuckMultiplier();

    /** Multiplicador de XP de pesca atualmente ativo (1.0 = sem boost). */
    double getXpMultiplier();
}
