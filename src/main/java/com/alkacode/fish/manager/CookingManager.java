package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Sistema de culinária: converte peixes crus em filetes cozidos. */
public final class CookingManager {

    private final AlkaFishPlugin plugin;

    public CookingManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("cooking.enabled", true);
    }

    public boolean isFishItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "alkafish_id"));
    }

    public String getFishId(ItemStack item) {
        if (!isFishItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "alkafish_id"), PersistentDataType.STRING);
    }

    /** Cria o filete cozido a partir de um peixe capturado. */
    public ItemStack createCookedFillet(Fish fish) {
        Material mat = switch (fish.getRarity()) {
            case COMMON, UNCOMMON -> Material.COOKED_COD;
            case RARE, EPIC -> Material.COOKED_SALMON;
            case LEGENDARY, MYTHIC -> Material.COOKED_COD;
        };
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.displayName(com.alkacode.fish.util.FishUtil.parse("<gold>Filete de " + fish.getDisplayName()));
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "alkafish_cooked"),
                    PersistentDataType.STRING, fish.getId());
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "alkafish_cooked_rarity"),
                    PersistentDataType.STRING, fish.getRarity().name());
        });
        return item;
    }
}
