package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** GUI de classes de armadura de pesca. */
public final class ClassGui extends FishGui {

    public ClassGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🛡 Classes de Pesca", Category.PROGRESSION, 3, "alkafish-class");
    }

    @Override
    public void render() {
        fillBlack();
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        int slot = 10;

        for (FishingClass fc : plugin.getFishingClassManager().getAllClasses()) {
            if (slot > 16) break;

            boolean has = stats.getUnlockedClasses().contains(fc.getId());
            boolean equipped = fc.getId().equals(stats.getActiveClassId());
            boolean canUpgrade = plugin.getFishingClassManager().canUpgrade(player, fc.getId());
            boolean canEquip = has && !equipped;
            boolean needPrevious = !has && !canUpgrade;

            String state = equipped ? "equipped" : (has ? "has" : (canUpgrade ? "buy" : (needPrevious ? "previous" : "need")));
            ItemStack item = fc.getMenuItem(state);
            if (item == null) {
                item = new ItemStack(Material.LEATHER_CHESTPLATE);
                if (fc.getArmorColor() != null) {
                    ItemStack finalItem = item;
                    finalItem.editMeta(meta -> {
                        if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leather) {
                            leather.setColor(fc.getArmorColor());
                        }
                    });
                    item = finalItem;
                }
            }

            final String classId = fc.getId();
            setItem(slot, item, e -> {
                if (equipped) {
                    player.sendMessage(plugin.getMessages().parse("class.already-equipped"));
                } else if (canEquip) {
                    plugin.getFishingClassManager().equipClass(player, classId);
                    player.closeInventory();
                } else if (canUpgrade) {
                    plugin.getFishingClassManager().upgradeClass(player, classId);
                    player.closeInventory();
                } else {
                    player.sendMessage(plugin.getMessages().parse("class.need-previous"));
                }
            });
            slot++;
        }

        setItem(18, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishingAreaGui(plugin, player).open());
        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }
}
