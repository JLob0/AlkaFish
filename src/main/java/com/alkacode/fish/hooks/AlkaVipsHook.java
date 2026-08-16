package com.alkacode.fish.hooks;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Integração com AlkaVips via ServicesManager (perks de pesca). */
public final class AlkaVipsHook extends HookBase {

    private Object api;
    private Method hasVip;

    public AlkaVipsHook(AlkaFishPlugin plugin) {
        super(plugin);
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("AlkaVips")) return;
            Class<?> apiClass = Class.forName("com.alkacode.vips.api.AlkaVipsAPI");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) return;
            this.api = registration.getProvider();
            this.hasVip = apiClass.getMethod("hasVip", UUID.class, String.class);
        } catch (Throwable e) {
            plugin.getLogger().warning("AlkaVips hook falhou: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    /** true se o jogador tiver a VIP com id informado. */
    public CompletableFuture<Boolean> hasVip(UUID uuid, String vipId) {
        try {
            if (api != null && hasVip != null) {
                Object result = hasVip.invoke(api, uuid, vipId);
                if (result instanceof CompletableFuture<?> cf) {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Boolean> casted = (CompletableFuture<Boolean>) cf;
                    return casted;
                }
            }
        } catch (Throwable ignored) {}
        return CompletableFuture.completedFuture(false);
    }

    /** Bônus de sorte de pesca da VIP "fishing_luck" (configurável por vip id). */
    public double getFishingLuckBonus(UUID uuid) {
        String vipId = plugin.getConfig().getString("integrations.alkavips.luck-vip-id", "vip_fisher");
        return hasVip(uuid, vipId).join() ? plugin.getConfig().getDouble("integrations.alkavips.luck-bonus", 0.05) : 0.0;
    }
}
