package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gerencia varas de pesca (carrega rods.yml, upa/repara, dá item). */
public final class RodManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, FishingRod> rodsById = new HashMap<>();
    private final List<FishingRod> rodsByLevel = new ArrayList<>();

    public RodManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rodsById.clear();
        rodsByLevel.clear();
        File file = new File(plugin.getDataFolder(), "rods.yml");
        if (!file.exists()) plugin.saveResource("rods.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection rodSection = config.getConfigurationSection("rods");
        if (rodSection == null) return;

        for (String id : rodSection.getKeys(false)) {
            ConfigurationSection section = rodSection.getConfigurationSection(id);
            if (section == null) continue;
            try {
                List<FishingRod.RodReward> rewards = new ArrayList<>();
                for (String r : section.getStringList("rewards")) {
                    String[] parts = r.split(",");
                    if (parts.length == 2) {
                        rewards.add(new FishingRod.RodReward(Double.parseDouble(parts[0]), parts[1]));
                    }
                }
                FishingRod rod = new FishingRod(
                        id,
                        section.getInt("level", 1),
                        section.getString("display-name", id),
                        org.bukkit.Material.valueOf(section.getString("material", "FISHING_ROD").toUpperCase()),
                        section.getInt("custom-model-data", 0),
                        section.getDouble("supported-weight", 5.0),
                        section.getInt("delay-seconds", 8),
                        section.getBoolean("break-on-heavy", true),
                        section.getBoolean("unbreakable", false),
                        section.getDouble("break-chance", 0.0),
                        section.getBoolean("allow-auto-sell", false),
                        section.getDouble("repair-cost.coins", 0),
                        section.getDouble("repair-cost.nacar", 0),
                        section.getDouble("upgrade-cost.coins", 0),
                        section.getDouble("upgrade-cost.nacar", 0),
                        section.getInt("enchant-base-cost", 10),
                        rewards,
                        section.getString("item.name", id),
                        section.getStringList("item.lore"));
                rodsById.put(id, rod);
                rodsByLevel.add(rod);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar vara '" + id + "': " + e.getMessage());
            }
        }
        rodsByLevel.sort(Comparator.comparingInt(FishingRod::getLevel));
    }

    public FishingRod getRodById(String id) {
        return rodsById.get(id);
    }

    public FishingRod getDefaultRod() {
        return rodsByLevel.isEmpty() ? null : rodsByLevel.get(0);
    }

    public FishingRod getNextRod(FishingRod rod) {
        int idx = rodsByLevel.indexOf(rod);
        if (idx < 0 || idx + 1 >= rodsByLevel.size()) return null;
        return rodsByLevel.get(idx + 1);
    }

    public boolean canUpgrade(Player player, FishingRod current) {
        FishingRod next = getNextRod(current);
        if (next == null) return false;
        double coins = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "coins");
        if (coins < next.getUpgradeCostCoins()) return false;
        double nacar = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "nacar");
        return nacar >= next.getUpgradeCostNacar();
    }

    public boolean upgradeRod(Player player, FishingRod current) {
        FishingRod next = getNextRod(current);
        if (next == null) return false;
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        plugin.getEconomyBridge().withdraw(player.getUniqueId(), "coins", next.getUpgradeCostCoins());
        plugin.getEconomyBridge().withdraw(player.getUniqueId(), "nacar", next.getUpgradeCostNacar());
        stats.setRodId(next.getId());
        stats.setRodLevel(next.getLevel());
        stats.setRodNacarEarned(0); // vara nova - contador de nacar pescado com ela reinicia
        giveRodItem(player, next);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("rod.upgraded",
                java.util.Map.of("level", String.valueOf(next.getLevel()))));
        return true;
    }

    /** Tenta upar automaticamente 1 tier se o jogador já tem coins+nacar suficientes
     * (feature de auto-upgrade, gated por perk VIP - ver toggleAutoUpgrade). */
    public boolean tryAutoUpgrade(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        if (!stats.isAutoUpgradeEnabled()) return false;
        // Revalida o perk aqui tambem (nao so na GUI) - se o VIP expirou depois do
        // jogador ligar o toggle, o auto-upgrade tem que parar de rodar sozinho.
        if (plugin.getAlkaVipsHook() == null || !plugin.getAlkaVipsHook().isAvailable()
                || !plugin.getAlkaVipsHook().hasPerk(player.getUniqueId(), "auto-upgrade-rod")) {
            return false;
        }
        FishingRod current = getRodById(stats.getRodId());
        if (current == null || !canUpgrade(player, current)) return false;
        boolean upgraded = upgradeRod(player, current);
        if (upgraded) {
            player.sendMessage(plugin.getMessages().parse("rod.auto-upgraded"));
        }
        return upgraded;
    }

    public boolean canRepair(Player player, FishingRod rod) {
        if (rod == null) return false;
        double coins = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "coins");
        if (coins < rod.getRepairCostCoins()) return false;
        double nacar = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "nacar");
        return nacar >= rod.getRepairCostNacar();
    }

    public boolean repairRod(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod rod = getRodById(stats.getRodId());
        if (rod == null) rod = getDefaultRod();
        if (rod == null) return false;
        plugin.getEconomyBridge().withdraw(player.getUniqueId(), "coins", rod.getRepairCostCoins());
        plugin.getEconomyBridge().withdraw(player.getUniqueId(), "nacar", rod.getRepairCostNacar());
        stats.setRodBroken(false);
        giveRodItem(player, rod);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        player.sendMessage(plugin.getMessages().parse("rod.repaired"));
        return true;
    }

    /** Custo em nacar do próximo tier - denominador dinâmico da barra {nacar_has}/{nacar_next}
     * no lore da vara (mostra progresso real até dar pra upar, não um valor fixo qualquer). */
    public double nextUpgradeCostNacar(FishingRod current) {
        FishingRod next = getNextRod(current);
        return next != null ? next.getUpgradeCostNacar() : 0;
    }

    /** Liga/desliga o auto-upgrade do jogador (gated por perk VIP fora daqui). */
    public void toggleAutoUpgrade(Player player) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        stats.setAutoUpgradeEnabled(!stats.isAutoUpgradeEnabled());
        plugin.getPlayerDataManager().save(player.getUniqueId());
    }

    /** Todas as varas com nível <= o nível atual do jogador (já "passadas" na progressão -
     * elegíveis pra virar skin visual da vara equipada). */
    public List<FishingRod> getUnlockedRods(FishingRod current) {
        List<FishingRod> out = new ArrayList<>();
        for (FishingRod r : rodsByLevel) {
            if (r.getLevel() <= current.getLevel()) out.add(r);
        }
        return out;
    }

    /** Custom-model-data a usar na exibição: a skin escolhida (se ainda válida - nível <=
     * vara real) ou 0 pra usar a própria skin da vara real. */
    private int resolveSkinCustomModelData(com.alkacode.fish.model.PlayerFishStats stats, FishingRod rod) {
        String skinId = stats.getRodSkinId();
        if (skinId == null || skinId.isEmpty()) return 0;
        FishingRod skin = rodsById.get(skinId);
        if (skin == null || skin.getLevel() > rod.getLevel()) return 0;
        return skin.getCustomModelData();
    }

    /** Define a skin visual da vara (id de uma vara já desbloqueada) e atualiza o item na
     * mão na hora. Id vazio/null reseta pra skin própria da vara atual. */
    public void setRodSkin(Player player, String skinId) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        stats.setRodSkinId(skinId);
        plugin.getPlayerDataManager().save(player.getUniqueId());
        refreshRodItem(player);
    }

    /** Dá a vara no slot configurado (rod-slot). */
    public void giveRodItem(Player player, FishingRod rod) {
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        ItemStack item = rod.toItemStack(plugin, stats.getRodEnchantLevels(),
                stats.getRodNacarEarned(), nextUpgradeCostNacar(rod), resolveSkinCustomModelData(stats, rod));
        int slot = getRodSlot();
        player.getInventory().setItem(slot, item);
        // Sem isso a vara ficava SO na hotbar, nao necessariamente na mao - se o jogador
        // nao estivesse ja no slot certo, PlayerFishEvent nunca disparava (vanilla exige
        // segurar um item de vara de verdade) e isHoldingRod() no ciclo AFK falhava,
        // derrubando a sessao no primeiro tick.
        player.getInventory().setHeldItemSlot(slot);
    }

    /** Regrava só o conteúdo (lore/nacar) da vara já equipada, sem forçar seleção de slot -
     * usado depois de cada captura pra {nacar_has}/{nacar_next} no tooltip não ficar
     * congelado no valor de quando a vara foi dada (giveRodItem só roda em upar/reparar/
     * entrar na área, não a cada peixe). */
    public void refreshRodItem(Player player) {
        int slot = getRodSlot();
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || !current.hasItemMeta()) return;
        if (!current.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_rod_id"))) return;

        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod rod = getRodById(stats.getRodId());
        if (rod == null) return;
        ItemStack updated = rod.toItemStack(plugin, stats.getRodEnchantLevels(),
                stats.getRodNacarEarned(), nextUpgradeCostNacar(rod), resolveSkinCustomModelData(stats, rod));
        player.getInventory().setItem(slot, updated);
    }

    /** Remove a vara do slot configurado (rod-slot) se ela for do AlkaFish. */
    public void removeRodItem(Player player) {
        int slot = getRodSlot();
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || !current.hasItemMeta()) return;
        if (current.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_rod_id"))) {
            player.getInventory().setItem(slot, null);
        }
    }

    private int getRodSlot() {
        int slot = plugin.getConfig().getInt("rods.rod-slot", 0);
        return (slot < 0 || slot >= 36) ? 0 : slot;
    }

    /** true se o jogador está segurando uma vara de pesca do AlkaFish na mão. */
    public boolean isHoldingRod(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_rod_id"));
    }
}
