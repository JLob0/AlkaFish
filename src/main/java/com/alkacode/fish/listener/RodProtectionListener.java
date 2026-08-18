package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Trava a vara e o botão de menu no inventário do jogador (não deixa mover, arrastar
 * ou dropar) - eles ficam presos no slot fixo configurado (rods.rod-slot/menu-slot)
 * o tempo todo que o jogador estiver na área. Não é o GuiListener do AlkaCore (R1) -
 * isso protege o inventário DE VERDADE do jogador, não uma tela de BaseGui.
 */
public final class RodProtectionListener implements Listener {

    private final AlkaFishPlugin plugin;

    public RodProtectionListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // Só nos importa o inventário DE VERDADE do jogador - uma tela de BaseGui aberta
        // já é toda cancelada pelo GuiListener do AlkaCore, não precisa duplicar aqui.
        if (event.getClickedInventory() != player.getInventory()) return;
        if (isLocked(event.getCurrentItem()) || isLocked(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (isLocked(event.getView().getItem(rawSlot))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isLocked(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private boolean isLocked(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(new NamespacedKey(plugin, "alkafish_rod_id"))
                || pdc.has(new NamespacedKey(plugin, "alkafish_menu"));
    }
}
