package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Carrega e descarrega os dados de pesca dos jogadores. */
public final class PlayerJoinQuitListener implements Listener {

    private final AlkaFishPlugin plugin;

    public PlayerJoinQuitListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
        plugin.getTensionGameManager().stopGame(event.getPlayer());
    }
}
