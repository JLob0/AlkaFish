package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.PlayerFishStats;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/** Lida com progressão de nível e XP dos jogadores. */
public final class LevelManager {

    private final AlkaFishPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public LevelManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Adiciona XP e processa level ups (loop de multi-level). Retorna a quantidade
     * de níveis subidos pra mensagens.
     */
    public int addXp(Player player, PlayerFishStats stats, double xp) {
        int maxLevel = plugin.getConfig().getInt("leveling.max-level", 50);
        int levelsBefore = stats.getLevel();
        stats.setXp(stats.getXp() + xp);
        double needed = PlayerFishStats.getXpForLevel(stats.getLevel() + 1);
        while (stats.getLevel() < maxLevel && stats.getXp() >= needed) {
            stats.setXp(stats.getXp() - needed);
            stats.setLevel(stats.getLevel() + 1);
            needed = PlayerFishStats.getXpForLevel(stats.getLevel() + 1);
        }
        if (stats.getLevel() >= maxLevel) {
            stats.setXp(Math.min(stats.getXp(), PlayerFishStats.getXpForLevel(maxLevel + 1)));
        }
        int gained = stats.getLevel() - levelsBefore;
        if (gained > 0) {
            player.sendMessage(mm.deserialize(
                    "<gold><bold>LEVEL UP! <yellow>Você alcançou o nível <aqua>" + stats.getLevel() + "<yellow>!"));
            plugin.getPlayerDataManager().save(player.getUniqueId());
        }
        return gained;
    }
}
