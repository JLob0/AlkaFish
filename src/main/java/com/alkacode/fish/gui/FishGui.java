package com.alkacode.fish.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Base comum das GUIs do AlkaFish: título com gradiente e painel preto. */
public abstract class FishGui extends BaseGui {

    protected final AlkaFishPlugin plugin;

    protected FishGui(AlkaFishPlugin plugin, Player player, String title, int rows, String id) {
        super(plugin, player, "<gradient:#0fa3b1:#b5e2fa>" + title, rows, id);
        this.plugin = plugin;
    }

    protected void fillBlack() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black>♥");
        fillBorder(glass);
    }
}
