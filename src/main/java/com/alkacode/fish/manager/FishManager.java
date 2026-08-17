package com.alkacode.fish.manager;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.FishRarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Carrega e disponibiliza os peixes definidos em fish.yml. */
public final class FishManager {

    private final AlkaFishPlugin plugin;
    private final Map<String, Fish> fishById = new HashMap<>();
    private final Map<FishRarity, List<Fish>> fishByRarity = new EnumMap<>(FishRarity.class);

    public FishManager(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        fishById.clear();
        fishByRarity.clear();
        for (FishRarity r : FishRarity.values()) {
            fishByRarity.put(r, new ArrayList<>());
        }

        File file = new File(plugin.getDataFolder(), "fish.yml");
        if (!file.exists()) {
            plugin.saveResource("fish.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection fishSection = config.getConfigurationSection("fish");
        if (fishSection == null) {
            plugin.getLogger().warning("fish.yml vazio ou inválido!");
            return;
        }

        for (String id : fishSection.getKeys(false)) {
            ConfigurationSection section = fishSection.getConfigurationSection(id);
            if (section == null) continue;
            try {
                FishRarity rarity = FishRarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());
                Fish fish = new Fish(
                        id,
                        section.getString("display-name", id),
                        rarity,
                        section.getStringList("biomes"),
                        section.getDouble("min-length", 10.0),
                        section.getDouble("max-length", 50.0),
                        section.getDouble("min-weight", 0.1),
                        section.getDouble("max-weight", 1.0),
                        section.getDouble("base-price", 5.0),
                        section.getString("items-adder-id", ""),
                        section.getInt("custom-model-data", 0),
                        section.getBoolean("requires-night", false),
                        section.getBoolean("requires-rain", false),
                        section.getInt("min-depth", 1),
                        section.getDouble("xp-reward", 10.0),
                        section.getStringList("lore"));
                fishById.put(id, fish);
                fishByRarity.get(rarity).add(fish);
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar peixe '" + id + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Carregados " + fishById.size() + " peixes.");
    }

    public Fish getFishById(String id) {
        return fishById.get(id);
    }

    public List<Fish> getFishForConditions(String biome, boolean isNight, boolean isRaining, int depth) {
        String normalizedBiome = normalizeBiome(biome);
        List<Fish> result = new ArrayList<>();
        for (Fish fish : fishById.values()) {
            if (!matchesBiome(fish.getBiomes(), normalizedBiome)) continue;
            if (fish.isRequiresNight() && !isNight) continue;
            if (fish.isRequiresRain() && !isRaining) continue;
            if (depth < fish.getMinDepth()) continue;
            result.add(fish);
        }
        return result;
    }

    /** Normaliza um bioma: remove o namespace (minecraft:) e deixa em MAIÚSCULAS.
     * Assim "minecraft:ocean" e "OCEAN" (do fish.yml) são tratados como iguais. */
    private String normalizeBiome(String biome) {
        if (biome == null) return "";
        String s = biome;
        int idx = s.indexOf(':');
        if (idx >= 0) s = s.substring(idx + 1);
        return s.toUpperCase();
    }

    private boolean matchesBiome(List<String> configured, String normalizedBiome) {
        for (String b : configured) {
            String nb = normalizeBiome(b);
            if (nb.equals(normalizedBiome) || nb.equals("ANY")) return true;
        }
        return false;
    }

    public Collection<Fish> getAllFish() {
        return fishById.values();
    }

    public List<Fish> getFishByRarity(FishRarity rarity) {
        return fishByRarity.getOrDefault(rarity, List.of());
    }
}
