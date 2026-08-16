package com.alkacode.fish.api;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.PlayerFishStats;

import java.util.UUID;

/** API pública do AlkaFish para outros plugins. */
public final class AlkaFishAPI {

    private final AlkaFishPlugin plugin;

    public AlkaFishAPI(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerFishStats getPlayerStats(UUID uuid) {
        return plugin.getPlayerDataManager().getStats(uuid);
    }

    public int getPlayerLevel(UUID uuid) {
        return getPlayerStats(uuid).getLevel();
    }

    public double getPlayerXp(UUID uuid) {
        return getPlayerStats(uuid).getXp();
    }

    public int getTotalCaught(UUID uuid) {
        return getPlayerStats(uuid).getTotalCaught();
    }

    public boolean isInTournament() {
        return plugin.getTournamentManager().getActiveTournament() != null;
    }

    public double getActiveSellMultiplier() {
        return plugin.getTournamentManager().getActiveSellMultiplier();
    }
}
