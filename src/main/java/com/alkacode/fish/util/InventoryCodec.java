package com.alkacode.fish.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Serializa ItemStack[] pra String (YAML por slot + Base64) e volta - mesmo padrão do
 * InventoryCodec do AlkaEssentials (InvRestore), evita as classes ObjectOutputStream
 * depreciadas do Bukkit. */
public final class InventoryCodec {

    private InventoryCodec() {}

    public static String encode(ItemStack[] items) {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            serialized.add(item != null ? item.serialize() : null);
        }
        config.set("items", serialized);
        return Base64.getEncoder().encodeToString(config.saveToString().getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static ItemStack[] decode(String source) {
        String yaml = new String(Base64.getDecoder().decode(source), StandardCharsets.UTF_8);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
        List<?> raw = config.getList("items");
        if (raw == null) return new ItemStack[0];
        ItemStack[] items = new ItemStack[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            Object entry = raw.get(i);
            items[i] = entry instanceof Map ? ItemStack.deserialize((Map<String, Object>) entry) : null;
        }
        return items;
    }
}
