package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Integração com AlkaShop via ServicesManager (venda de itens). */
public final class AlkaShopHook extends HookBase {

    private Object api;
    private Method isAutoSellActive;
    private Method sellItems;
    private Method notifyAutoSell;

    public AlkaShopHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaShop")) return;
            Class<?> apiClass = Class.forName("com.alkacode.shop.api.AlkaShopAPI");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return;
            this.api = registration.getProvider();
            this.isAutoSellActive = apiClass.getMethod("isAutoSellActive", Player.class);
            this.sellItems = apiClass.getMethod("sellItems", Player.class, List.class);
            this.notifyAutoSell = apiClass.getMethod("notifyAutoSell", Player.class, Map.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaShop hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    public boolean isAutoSellActive(Player player) {
        try {
            if (api != null && isAutoSellActive != null) {
                Object result = isAutoSellActive.invoke(api, player);
                return Boolean.TRUE.equals(result);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** Vende uma lista de itens, devolvendo totais por moeda. */
    public Map<String, Double> sellItems(Player player, List<ItemStack> items) {
        try {
            if (api != null && sellItems != null) {
                Object result = sellItems.invoke(api, player, items);
                @SuppressWarnings("unchecked")
                Map<String, Double> totals = (Map<String, Double>) result;
                return totals;
            }
        } catch (Throwable ignored) {}
        return Map.of();
    }

    public void notifyAutoSell(Player player, Map<String, Double> totals) {
        try {
            if (api != null && notifyAutoSell != null) {
                notifyAutoSell.invoke(api, player, totals);
            }
        } catch (Throwable ignored) {}
    }
}
