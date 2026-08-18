package com.alkacode.fish.hooks;

import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Bukkit;

import java.util.UUID;

/** Bridge para a AlkaEconomy (hard depend) - múltiplas moedas (coins, etc). */
public final class AlkaEconomyBridge {

    private final EconomyManager manager;

    public AlkaEconomyBridge() {
        this.manager = ((AlkaEconomyPlugin) Bukkit.getPluginManager().getPlugin("AlkaEconomy"))
                .getEconomyManager();
    }

    public boolean isValidCurrency(String currency) {
        return manager.isValidCurrency(currency);
    }

    public double getBalance(UUID uuid, String currency) {
        return manager.getBalance(uuid, currency);
    }

    public boolean has(UUID uuid, String currency, double amount) {
        return manager.has(uuid, currency, amount);
    }

    public boolean withdraw(UUID uuid, String currency, double amount) {
        if (!has(uuid, currency, amount)) return false;
        manager.removeBalance(uuid, currency, amount);
        return true;
    }

    public void deposit(UUID uuid, String currency, double amount) {
        manager.addBalance(uuid, currency, amount);
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }

    /** Top N saldos de uma moeda - consulta direta no banco (jogadores offline entram
     * também), pensada pra tela de TOP, não pra hot path. */
    public java.util.List<com.alkacode.economy.storage.EconomyRepository.TopBalanceEntry> getTopBalances(String currency, int limit) {
        return manager.getTopBalances(currency, limit);
    }
}
