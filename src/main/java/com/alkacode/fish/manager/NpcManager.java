package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

/** Gerencia o NPC de pesca via Citizens + DecentHolograms (reflection). */
public final class NpcManager {

    private final AlkaFishPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private Object npcInstance;
    private int npcId = -1;
    private boolean citizensAvailable = false;

    public NpcManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        checkCitizens();
        if (plugin.getConfig().getBoolean("npc.enabled", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, this::spawnNpc, 40L);
        }
    }

    private void checkCitizens() {
        citizensAvailable = Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }

    public void spawnNpc() {
        if (!citizensAvailable) {
            plugin.getLogger().warning("Citizens não está habilitado. NPC de pesca não será spawnado.");
            return;
        }
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "npc.yml"));
            if (!config.getBoolean("enabled", true)) return;

            String worldName = config.getString("location.world", "world");
            Location loc = new Location(
                    Bukkit.getWorld(worldName),
                    config.getDouble("location.x", 0),
                    config.getDouble("location.y", 64),
                    config.getDouble("location.z", 0),
                    (float) config.getDouble("location.yaw", 0),
                    (float) config.getDouble("location.pitch", 0));
            String npcName = config.getString("name", "&6&lPESCADOR").replace('&', '§');
            String skin = config.getString("skin", "Notch");

            Class<?> registryClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = registryClass.getMethod("getNPCRegistry").invoke(null);
            Object npc = registry.getClass().getMethod("createNPC", org.bukkit.entity.EntityType.class, String.class)
                    .invoke(registry, org.bukkit.entity.EntityType.PLAYER, npcName);
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, loc);
            npcId = (int) npc.getClass().getMethod("getId").invoke(npc);
            npcInstance = npc;

            if (!skin.isEmpty()) {
                Class<?> skinTraitClass = Class.forName("net.citizensnpcs.trait.SkinTrait");
                Object skinTrait = npc.getClass().getMethod("getOrAddTrait", Class.class).invoke(npc, skinTraitClass);
                skinTraitClass.getMethod("setSkinName", String.class).invoke(skinTrait, skin);
            }

            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")
                    && config.getBoolean("hologram.enabled", true)) {
                spawnHologram(loc.clone().add(0, config.getDouble("hologram.offset-y", 3.0), 0), config);
            }
            plugin.getLogger().info("NPC de pesca spawnado em " + loc);
        } catch (Throwable e) {
            plugin.getLogger().warning("Falha ao spawnar NPC: " + e.getMessage());
        }
    }

    private void spawnHologram(Location loc, FileConfiguration config) {
        try {
            Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Object hologram = dhapiClass.getMethod("createHologram", String.class, Location.class)
                    .invoke(null, "alkafish_npc_" + UUID.randomUUID().toString().substring(0, 8), loc);
            Method addLine = dhapiClass.getMethod("addHologramLine",
                    Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram"), String.class);
            for (String line : config.getStringList("hologram.lines")) {
                addLine.invoke(null, hologram, line.replace('&', '§'));
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("Falha ao criar holograma: " + e.getMessage());
        }
    }

    public void removeNpc() {
        if (npcId == -1 || !citizensAvailable) return;
        try {
            Class<?> registryClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = registryClass.getMethod("getNPCRegistry").invoke(null);
            Object npc = registry.getClass().getMethod("getById", int.class).invoke(registry, npcId);
            if (npc != null) npc.getClass().getMethod("destroy").invoke(npc);
        } catch (Throwable e) {
            plugin.getLogger().warning("Falha ao remover NPC: " + e.getMessage());
        }
    }

    public void handleNpcClick(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "npc.yml"));
        String cmd = config.getString("click-command", "fish");
        String msg = config.getString("click-message", "");
        if (!msg.isEmpty()) player.sendMessage(mm.deserialize(msg));
        player.performCommand(cmd);
    }
}
