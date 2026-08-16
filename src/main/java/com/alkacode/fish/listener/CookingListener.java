package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

/** Converte peixes crus em filetes cozidos na fornalha. */
public final class CookingListener implements Listener {

    private final AlkaFishPlugin plugin;

    public CookingListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!plugin.getCookingManager().isEnabled()) return;
        ItemStack source = event.getSource();
        if (!plugin.getCookingManager().isFishItem(source)) return;

        String fishId = plugin.getCookingManager().getFishId(source);
        if (fishId == null) return;

        Fish fish = plugin.getFishManager().getFishById(fishId);
        if (fish == null) return;

        event.setResult(plugin.getCookingManager().createCookedFillet(fish));
    }
}
