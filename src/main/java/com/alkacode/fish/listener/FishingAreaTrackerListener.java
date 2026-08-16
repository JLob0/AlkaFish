package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Marca entrada/saída na área de pesca (igual ao PlayerMineTrackerListener das minas):
 * ao entrar, marca o início da sessão e dá a vara; ao sair, limpa efeitos de classe
 * e finaliza a sessão. Ignora micro-movimentos de câmera (mudança de bloco).
 */
public final class FishingAreaTrackerListener implements Listener {

    private final AlkaFishPlugin plugin;
    private final Set<UUID> inArea = ConcurrentHashMap.newKeySet();

    public FishingAreaTrackerListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        update(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        update(event.getPlayer(), event.getTo());
    }

    private void update(Player player, Location to) {
        boolean inside = plugin.getFishingAreaManager().isInArea(to);
        boolean wasInside = inArea.contains(player.getUniqueId());
        if (inside == wasInside) return;

        if (inside) {
            inArea.add(player.getUniqueId());
            plugin.getFishingAreaManager().markEnter(player);
            giveRod(player);
        } else {
            inArea.remove(player.getUniqueId());
            plugin.getFishingAreaManager().markLeave(player);
            plugin.getFishingClassManager().clearClassEffects(player);
            plugin.getRodManager().removeRodItem(player);
        }
    }

    private void giveRod(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        if (rod != null) plugin.getRodManager().giveRodItem(player, rod);
    }
}
