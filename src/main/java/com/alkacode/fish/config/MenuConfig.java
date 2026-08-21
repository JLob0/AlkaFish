package com.alkacode.fish.config;

import com.alkacode.fish.AlkaFishPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Centraliza icone/nome/lore/titulo das GUIs no menus.yml (posicao continua em
 * gui-layouts.yml, ver GuiLayoutLoader). Itens sao definidos por menus.yml.<caminho>
 * com material/name/lore; placeholders passados como {chave} sao substituidos na hora.
 */
public final class MenuConfig {

    private final AlkaFishPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public MenuConfig(AlkaFishPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "menus.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        mergeMissingDefaults();
    }

    /** Adiciona chaves novas do menus.yml do jar ao arquivo salvo (migracao de versao). */
    private void mergeMissingDefaults() {
        try (InputStream in = plugin.getResource("menus.yml")) {
            if (in == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao migrar menus.yml", e);
        }
    }

    public String title(String path, Map<String, String> placeholders) {
        return apply(config.getString(path + ".title", ""), placeholders);
    }

    /** Constroi o ItemStack a partir de menus.yml.<path> (material/name/lore) com placeholders. */
    public ItemStack item(String path, Map<String, String> placeholders) {
        return item(path, placeholders, List.of());
    }

    /** Igual a {@link #item(String, Map)}, mas anexa linhas extra de lore geradas em Java
     * (conteudo genuinamente dinamico - listas de tamanho variavel, ex: top 3 de um ranking -
     * que nao cabem num template de placeholder por linha). */
    public ItemStack item(String path, Map<String, String> placeholders, List<Component> extraLore) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return new ItemStack(Material.BARRIER);
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = name(path, placeholders);
        if (name != null && !name.isEmpty()) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
        }
        List<Component> lore = new ArrayList<>(lore(path, placeholders));
        lore.addAll(extraLore);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String name(String path, Map<String, String> placeholders) {
        return apply(config.getString(path + ".name", ""), placeholders);
    }

    public List<Component> lore(String path, Map<String, String> placeholders) {
        List<Component> loreList = new ArrayList<>();
        for (String line : rawLore(path, placeholders)) {
            loreList.add(MiniMessage.miniMessage().deserialize(line));
        }
        return loreList;
    }

    /** Lore em MiniMessage cru (String, nao Component) - usado por helpers do BaseGui como
     * head()/createItem() que recebem lore como String... em vez de Component. */
    public List<String> rawLore(String path, Map<String, String> placeholders) {
        List<String> loreList = new ArrayList<>();
        for (String line : config.getStringList(path + ".lore")) {
            loreList.add(apply(line, placeholders));
        }
        return loreList;
    }

    private static String apply(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
