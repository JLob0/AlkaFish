package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.RodEnchantment;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** GUI de encantamentos da vara. */
public final class EnchantGui extends FishGui {

    public EnchantGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "✨ Encantamentos", 3, "alkafish-enchant");
    }

    @Override
    public void render() {
        fillBlack();
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        int slot = 10;

        for (RodEnchantment enc : plugin.getEnchantmentManager().getAllEnchantments()) {
            if (slot > 16) break;
            int currentLevel = stats.getRodEnchantLevel(enc.getId());
            boolean canUpgrade = plugin.getEnchantmentManager().canUpgrade(player, enc.getId());
            boolean maxed = enc.getMaxLevel() != -1 && currentLevel >= enc.getMaxLevel();
            int cost = enc.costForLevel(currentLevel);
            double bonus = enc.getMultiplierPerLevel() > 0
                    ? enc.multiplierBonus(currentLevel)
                    : enc.percentBonus(currentLevel);

            String state = maxed ? "max-level" : (canUpgrade ? "can-upgrade" : "cannot-upgrade");
            ItemStack item = enc.getMenuItem(state, currentLevel, enc.getMaxLevel(), bonus, cost);
            if (item == null) item = new ItemStack(Material.ENCHANTED_BOOK);

            final String enchantId = enc.getId();
            final boolean fMaxed = maxed;
            final boolean fCan = canUpgrade;
            setItem(slot, item, e -> {
                if (fMaxed) return;
                if (fCan) {
                    plugin.getEnchantmentManager().upgradeEnchantment(player, enchantId);
                    player.closeInventory();
                } else {
                    player.sendMessage(plugin.getMessages().parse("rod.not-enough-nacar",
                            java.util.Map.of("cost", String.valueOf(cost))));
                }
            });
            slot++;
        }

        setItem(18, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new RodGui(plugin, player).open());
        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }
}
