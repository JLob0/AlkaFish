package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Integração com ItemsAdder via reflection em CustomStack. */
public final class ItemsAdderHook extends HookBase {

    private Method getInstance;

    public ItemsAdderHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) return;
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            this.getInstance = customStackClass.getMethod("getInstance", String.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("ItemsAdder hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return getInstance != null;
    }

    public ItemStack getItem(String namespacedId) {
        try {
            if (getInstance != null) {
                Object stack = getInstance.invoke(null, namespacedId);
                if (stack != null) {
                    Method getItemStack = stack.getClass().getMethod("getItemStack");
                    return (ItemStack) getItemStack.invoke(stack);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
