package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingArea;
import com.alkacode.fish.model.FishingRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Gerencia a área de pesca pública única (região + spawn), igual ao AlkaMines:
 * o admin define via seleção do WorldEdit (//pos1///pos2) + spawn, e o jogador
 * usa /pesca para teleportar e pescar. Persiste em fishingarea.yml.
 */
public final class FishingAreaManager {

    private final AlkaFishPlugin plugin;
    private FishingArea area;
    private Location pos1;
    private Location pos2;

    public FishingAreaManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // --- seleção pos1/pos2 (sem depender do WorldEdit) ---
    public void setPos1(Location loc) {
        this.pos1 = loc.clone();
    }

    public void setPos2(Location loc) {
        this.pos2 = loc.clone();
    }

    public boolean hasSelection() {
        return pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld());
    }

    /** Monta uma FishingRegion a partir do pos1/pos2 selecionado. */
    public FishingRegion getSelectionRegion() {
        if (!hasSelection()) return null;
        return new FishingRegion(pos1.getWorld().getName(),
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "fishingarea.yml");
        if (!file.exists()) {
            plugin.saveResource("fishingarea.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getString("area.region.world") == null) {
            area = null;
            return;
        }
        String id = config.getString("area.id", "pesca");
        String displayName = config.getString("area.display-name", "<aqua>Área de Pesca");
        FishingRegion region = readRegion(config, "area.region");
        FishingRegion lobby = readRegion(config, "area.lobby");
        FishingArea a = new FishingArea(id, displayName, region);
        if (lobby != null) a.setLobbyRegion(lobby);
        if (config.get("area.spawn.world") != null) {
            a.setSpawn(readLocation(config, "area.spawn"));
        }
        if (config.get("area.exit.world") != null) {
            a.setExit(readLocation(config, "area.exit"));
        }
        this.area = a;
    }

    public void save() {
        if (area == null) return;
        File file = new File(plugin.getDataFolder(), "fishingarea.yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("area.id", area.getId());
        config.set("area.display-name", area.getDisplayName());
        writeRegion(config, "area.region", area.getRegion());
        if (area.getLobbyRegion() != null) writeRegion(config, "area.lobby", area.getLobbyRegion());
        if (area.getSpawn() != null) writeLocation(config, "area.spawn", area.getSpawn());
        if (area.getExit() != null) writeLocation(config, "area.exit", area.getExit());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao salvar fishingarea.yml: " + e.getMessage());
        }
    }

    public Optional<FishingArea> getArea() {
        return Optional.ofNullable(area);
    }

    public boolean isConfigured() {
        return area != null;
    }

    /** Define a área com a região (fallback do centro como spawn se ainda não houver). */
    public void setArea(String id, String displayName, FishingRegion region) {
        FishingArea a = new FishingArea(id, displayName, region);
        if (area != null) a.setLobbyRegion(area.getLobbyRegion());
        this.area = a;
        save();
    }

    public void setSpawn(Location loc) {
        if (area == null) return;
        area.setSpawn(loc.clone());
        save();
    }

    public void setLobby(FishingRegion region) {
        if (area == null) return;
        area.setLobbyRegion(region);
        save();
    }

    public void setExit(Location loc) {
        if (area == null) return;
        area.setExit(loc.clone());
        save();
    }

    /** Teleporta o jogador para a área, limpa o inventário e dá a vara + botão do menu. */
    public void teleportTo(Player player) {
        if (area == null) {
            player.sendMessage(plugin.getMessages().parse("area.not-configured"));
            return;
        }
        Location target = area.getSpawn() != null ? area.getSpawn() : area.getRegion().getCenter();
        player.teleport(target);

        // Só entra na área sem itens: salva o inventário, limpa e dá vara + botão de menu.
        saveAndClearInventory(player);

        // Dá a vara automaticamente ao chegar
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        if (rod != null) plugin.getRodManager().giveRodItem(player, rod);

        player.sendMessage(plugin.getMessages().parse("area.teleported", java.util.Map.of("area", area.getDisplayName())));
        if (plugin.getConfig().getBoolean("fishing-area.invisibility.auto-toggle", true)) {
            plugin.getFishingAreaManager().setInvisibility(player, true);
        }
    }

    /** Teleporta para a saída configurada (ou spawn do mundo) e restaura o inventário. */
    public void teleportExit(Player player) {
        Location exit = area != null && area.getExit() != null ? area.getExit() : null;
        if (exit == null) {
            World w = org.bukkit.Bukkit.getWorlds().get(0);
            exit = w.getSpawnLocation();
        }
        player.teleport(exit);
        if (plugin.getFishingAreaManager().isInvisible(player)) {
            plugin.getFishingAreaManager().setInvisibility(player, false);
        }
        plugin.getRodManager().removeRodItem(player);
        plugin.getFishingClassManager().clearClassEffects(player);
        restoreInventory(player);
        player.sendMessage(plugin.getMessages().parse("area.left"));
    }

    // --- inventário da área (só pode entrar sem itens) ---
    private final java.util.Map<java.util.UUID, org.bukkit.inventory.ItemStack[]> savedInventories = new java.util.concurrent.ConcurrentHashMap<>();

    /** Salva o inventário atual (se ainda não salvo), limpa tudo e dá o botão do menu. */
    public void saveAndClearInventory(Player player) {
        savedInventories.putIfAbsent(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
        giveMenuOpener(player);
    }

    /** Restaura o inventário salvo ao sair da área (se houver). */
    public void restoreInventory(Player player) {
        org.bukkit.inventory.ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved);
        }
    }

    /** Item de abertura do menu, fica no meio da hotbar para o jogador abrir a pesca
     * mesmo sem a vara (ex.: quando a vara quebra e some do inventário). */
    public org.bukkit.inventory.ItemStack createMenuOpener() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<aqua>🎣 Menu de Pesca"));
        meta.lore(java.util.List.of(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Clique com o botão direito para abrir")));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "alkafish_menu"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public void giveMenuOpener(Player player) {
        player.getInventory().setItem(4, createMenuOpener());
    }

    /** true se o item é o botão de abrir o menu de pesca. */
    public boolean isMenuOpener(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "alkafish_menu"));
    }

    /** true se a localização está dentro da REGIÃO PRINCIPAL de pesca (a água). */
    public boolean isInArea(Location loc) {
        return area != null && area.containsRegion(loc);
    }

    public boolean isInArea(Player player) {
        return isInArea(player.getLocation());
    }

    /** true se o jogador está dentro da REGIÃO PRINCIPAL (a água). */
    public boolean isPlayerInFishingArea(Player player) {
        return area != null && area.containsRegion(player.getLocation());
    }

    /** true se o hook está dentro da REGIÃO PRINCIPAL (a água). */
    public boolean isHookInFishingArea(org.bukkit.entity.FishHook hook) {
        return area != null && area.containsRegion(hook.getLocation());
    }

    /** true se a localização está no lobby OU na região principal (dá/tira a vara no tracker). */
    public boolean isInLobbyArea(Location loc) {
        return area != null && area.containsLobby(loc);
    }

    public boolean isInLobbyArea(Player player) {
        return isInLobbyArea(player.getLocation());
    }

    /** true se o local está dentro da área E o bloco (ou o bloco abaixo dele) é água
     * (para iniciar o AFK). O FishHook flutua na superfície, então o bloco exato pode
     * ser AIR mesmo com o hook na água. */
    public boolean isWaterInArea(Location loc) {
        if (!isInArea(loc)) return false;
        if (loc.getWorld() == null) return false;
        if (isWater(loc.getBlock().getType())) return true;
        return isWater(loc.clone().subtract(0, 1, 0).getBlock().getType());
    }

    private static boolean isWater(org.bukkit.Material type) {
        return type == org.bukkit.Material.WATER || type == org.bukkit.Material.WATER_CAULDRON;
    }

    // --- sessão de pesca (contadores/tempo) ---
    private final java.util.Map<java.util.UUID, Long> fishingStartTime = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Integer> fishCount = new java.util.concurrent.ConcurrentHashMap<>();

    public void incrementFishCount(Player player) {
        fishCount.merge(player.getUniqueId(), 1, Integer::sum);
    }

    public int getFishCount(Player player) {
        return fishCount.getOrDefault(player.getUniqueId(), 0);
    }

    public String getFishingTime(Player player) {
        Long start = fishingStartTime.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
        long seconds = (System.currentTimeMillis() - start) / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** Marca o início da sessão de pesca ao entrar na área. */
    public void markEnter(Player player) {
        fishingStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        fishCount.put(player.getUniqueId(), 0);
    }

    /** Limpa o estado da sessão ao sair da área. */
    public void markLeave(Player player) {
        fishingStartTime.remove(player.getUniqueId());
        fishCount.remove(player.getUniqueId());
    }

    public void shutdown() {
        clearAllInvisibility();
    }

    // --- invisibilidade (mantido do ATT) ---
    private final java.util.Set<java.util.UUID> invisiblePlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void setInvisibility(Player player, boolean invisible) {
        if (invisible) {
            invisiblePlayers.add(player.getUniqueId());
            for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) other.hidePlayer(plugin, player);
            }
            player.sendMessage(plugin.getMessages().parse("area.invisibility-on"));
        } else {
            invisiblePlayers.remove(player.getUniqueId());
            for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
                other.showPlayer(plugin, player);
            }
            player.sendMessage(plugin.getMessages().parse("area.invisibility-off"));
        }
    }

    public void toggleInvisibility(Player player) {
        setInvisibility(player, !invisiblePlayers.contains(player.getUniqueId()));
    }

    public boolean isInvisible(Player player) {
        return invisiblePlayers.contains(player.getUniqueId());
    }

    public void clearAllInvisibility() {
        for (java.util.UUID uuid : new java.util.HashSet<>(invisiblePlayers)) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) {
                invisiblePlayers.remove(uuid);
                for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
                    other.showPlayer(plugin, p);
                }
            }
        }
    }

    private FishingRegion readRegion(FileConfiguration config, String path) {
        String world = config.getString(path + ".world");
        if (world == null) return null;
        return new FishingRegion(world,
                config.getInt(path + ".x1"), config.getInt(path + ".y1"), config.getInt(path + ".z1"),
                config.getInt(path + ".x2"), config.getInt(path + ".y2"), config.getInt(path + ".z2"));
    }

    private void writeRegion(FileConfiguration config, String path, FishingRegion r) {
        config.set(path + ".world", r.getWorld());
        config.set(path + ".x1", r.getX1());
        config.set(path + ".y1", r.getY1());
        config.set(path + ".z1", r.getZ1());
        config.set(path + ".x2", r.getX2());
        config.set(path + ".y2", r.getY2());
        config.set(path + ".z2", r.getZ2());
    }

    private Location readLocation(FileConfiguration config, String path) {
        String world = config.getString(path + ".world");
        if (world == null) return null;
        return new Location(org.bukkit.Bukkit.getWorld(world),
                config.getDouble(path + ".x"), config.getDouble(path + ".y"), config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"), (float) config.getDouble(path + ".pitch"));
    }

    private void writeLocation(FileConfiguration config, String path, Location l) {
        config.set(path + ".world", l.getWorld() != null ? l.getWorld().getName() : "world");
        config.set(path + ".x", l.getX());
        config.set(path + ".y", l.getY());
        config.set(path + ".z", l.getZ());
        config.set(path + ".yaw", (double) l.getYaw());
        config.set(path + ".pitch", (double) l.getPitch());
    }
}
