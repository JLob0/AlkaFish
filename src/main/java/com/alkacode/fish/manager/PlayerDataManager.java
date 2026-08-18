package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.PlayerFishDataEntity;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cache em memória dos dados de pesca por jogador. */
public final class PlayerDataManager {

    private final AlkaFishPlugin plugin;
    private final Map<UUID, PlayerFishStats> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadAsync(player.getUniqueId());
        }
    }

    public void loadAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerFishDataEntity entity = plugin.getPlayerFishDataRepository().findByUuid(uuid).orElse(null);
                PlayerFishStats stats;
                if (entity == null) {
                    stats = new PlayerFishStats(uuid);
                    plugin.getPlayerFishDataRepository().save(toEntity(stats));
                } else {
                    stats = fromEntity(uuid, entity);
                }
                cache.put(uuid, stats);
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao carregar dados de pesca de " + uuid + ": " + e.getMessage());
            }
        });
    }

    /** Obtém stats do cache; se ainda não carregado, cria um novo (persistido no próximo save). */
    public PlayerFishStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> new PlayerFishStats(id));
    }

    public void save(UUID uuid) {
        PlayerFishStats stats = cache.get(uuid);
        if (stats == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getPlayerFishDataRepository().save(toEntity(stats));
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao salvar dados de pesca de " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void saveAllSync() {
        for (Map.Entry<UUID, PlayerFishStats> entry : cache.entrySet()) {
            try {
                plugin.getPlayerFishDataRepository().save(toEntity(entry.getValue()));
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao salvar dados de " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    /** Zera os dados de pesca do jogador (novo PlayerFishStats padrão) e salva. */
    public void reset(UUID uuid) {
        PlayerFishStats fresh = new PlayerFishStats(uuid);
        cache.put(uuid, fresh);
        save(uuid);
    }

    private PlayerFishDataEntity toEntity(PlayerFishStats s) {
        StringBuilder enchants = new StringBuilder();
        for (var e : s.getRodEnchantLevels().entrySet()) {
            if (enchants.length() > 0) enchants.append(";");
            enchants.append(e.getKey()).append(":").append(e.getValue());
        }
        String unlocked = String.join(";", s.getUnlockedClasses());
        return new PlayerFishDataEntity(
                s.getPlayerUuid(), s.getLevel(), s.getXp(), s.getTotalCaught(),
                s.getBiggestLength(), s.getBiggestFishId(), s.getTotalWeight(),
                s.getBagCapacity(), s.getCurrentBagWeight(),
                s.getRodId(), s.getRodLevel(), s.isRodBroken(),
                enchants.toString(), s.getActiveClassId(), unlocked,
                s.getRodNacarEarned(), s.getNacarNext(), s.isAutoUpgradeEnabled(), s.getSavedInventory(),
                s.getTotalFishingSeconds(), s.getRodSkinId(), s.isAutoSellOnFull());
    }

    private PlayerFishStats fromEntity(UUID uuid, PlayerFishDataEntity e) {
        PlayerFishStats s = new PlayerFishStats(uuid);
        s.setLevel(e.level());
        s.setXp(e.xp());
        s.setTotalCaught(e.totalCaught());
        s.setBiggestLength(e.biggestLength());
        s.setBiggestFishId(e.biggestFishId());
        s.setTotalWeight(e.totalWeight());
        s.setBagCapacity(e.bagCapacity());
        s.setCurrentBagWeight(e.currentBagWeight());
        s.setRodId(e.rodId());
        s.setRodLevel(e.rodLevel());
        s.setRodBroken(e.rodBroken());
        s.setActiveClassId(e.activeClassId());
        s.setRodNacarEarned(e.rodNacarEarned());
        s.setNacarNext(e.nacarNext());
        s.setAutoUpgradeEnabled(e.autoUpgradeEnabled());
        s.setSavedInventory(e.savedInventory());
        s.setTotalFishingSeconds(e.totalFishingSeconds());
        s.setRodSkinId(e.rodSkinId());
        s.setAutoSellOnFull(e.autoSellOnFull());
        if (e.rodEnchants() != null && !e.rodEnchants().isEmpty()) {
            for (String pair : e.rodEnchants().split(";")) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    try { s.setRodEnchantLevel(kv[0], Integer.parseInt(kv[1])); } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (e.unlockedClasses() != null && !e.unlockedClasses().isEmpty()) {
            for (String c : e.unlockedClasses().split(";")) {
                if (!c.isEmpty()) s.getUnlockedClasses().add(c);
            }
        }
        return s;
    }
}
