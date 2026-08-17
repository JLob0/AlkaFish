package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

    private void giveRod(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        if (rod == null) return;
        ItemStack rodItem = rod.toItemStack(plugin, stats.getRodEnchantLevels(), stats.getNacar(), stats.getNacarNext());
        PlayerInventory inv = player.getInventory();

        // Já tem a vara no inventário? Só seleciona, não duplica.
        int existing = findExisting(inv, rodItem);
        if (existing >= 0) {
            if (existing <= 8) inv.setHeldItemSlot(existing);
            return;
        }

        // Primeiro slot vazio da hotbar, senão do resto do inventário.
        int slot = firstEmptySlot(inv);
        if (slot < 0) {
            player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), rodItem);
            player.sendMessage(plugin.getMessages().parse("area.inventory-full"));
            return;
        }
        inv.setItem(slot, rodItem);
        if (slot <= 8) inv.setHeldItemSlot(slot);
    }

    private int findExisting(PlayerInventory inv, ItemStack rodItem) {
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.isSimilar(rodItem)) return i;
        }
        return -1;
    }

    private int firstEmptySlot(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i) == null) return i;
        }
        return -1;
    }
}
