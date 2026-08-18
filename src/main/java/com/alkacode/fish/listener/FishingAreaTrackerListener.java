package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

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
        boolean inside = plugin.getFishingAreaManager().isInLobbyArea(to);
        boolean wasInside = inArea.contains(player.getUniqueId());
        if (inside == wasInside) return;

        if (inside) {
            inArea.add(player.getUniqueId());
            plugin.getFishingAreaManager().markEnter(player);
            // Só entra na área sem itens: limpa e dá o botão do menu.
            plugin.getFishingAreaManager().saveAndClearInventory(player);
            giveRod(player);
        } else {
            inArea.remove(player.getUniqueId());
            plugin.getFishingAreaManager().markLeave(player);
            plugin.getFishingClassManager().clearClassEffects(player);
            plugin.getRodManager().removeRodItem(player);
            plugin.getFishingAreaManager().restoreInventory(player);
        }
    }

    /** Inventário já está vazio nesse ponto (saveAndClearInventory acabou de rodar) -
     * sempre delega pro RodManager, que usa o slot fixo configurado (rods.rod-slot).
     * Antes essa classe tinha lógica própria de "primeiro slot vazio", divergente do
     * RodManager e por isso a vara caía em slots diferentes dependendo de como foi dada. */
    private void giveRod(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        if (stats.isRodBroken()) {
            // Vara quebrada não volta pro inventário sozinha - precisa reparar primeiro
            // (senão o jogador recebe uma vara "quebrada" de volta sem saber o porquê).
            player.sendMessage(plugin.getMessages().parse("rod.broken-cannot-receive"));
            return;
        }
        FishingRod rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        if (rod == null) return;
        plugin.getRodManager().giveRodItem(player, rod);
    }
}
