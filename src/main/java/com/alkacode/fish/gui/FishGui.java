package com.alkacode.fish.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.gui.layout.GuiLayoutLoader;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/** Base comum das GUIs do AlkaFish: título com gradiente+bold (categoria do
 * MestreDEV-STYLE-GUIDE.md) e painel preto. */
public abstract class FishGui extends BaseGui {

    protected final AlkaFishPlugin plugin;

    /** Categorias de cor do style guide da rede - cada GUI escolhe a que combina com o que faz. */
    protected enum Category {
        /** Verde - marca/hub/ação rotineira. */
        BRAND("#55FFAA", "#00AA55"),
        /** Roxo - progressão (upar, evoluir, coletar). */
        PROGRESSION("#AA55FF", "#5500AA"),
        /** Dourado - ranking/destaque (torneio, recompensas). */
        HIGHLIGHT("#FFAA00", "#FFE055");

        final String start;
        final String end;

        Category(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    protected FishGui(AlkaFishPlugin plugin, Player player, String title, int rows, String id) {
        this(plugin, player, title, Category.BRAND, rows, id);
    }

    protected FishGui(AlkaFishPlugin plugin, Player player, String title, Category category, int rows, String id) {
        super(plugin, player, "<gradient:" + category.start + ":" + category.end + "><bold>"
                + title + "</bold></gradient>", rows, id);
        this.plugin = plugin;
    }

    protected void fillBlack() {
        fillBorder(menu().item("common.border", null));
    }

    /** Aplica o layout do YML: preenche a borda (#) com o icone de menus.yml.common.border
     * e retorna o layout, pra depois usar setAt(...) nos chars de conteudo. */
    protected GuiLayoutLoader.GuiLayout applyBorder(String layoutName) {
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout(layoutName);
        layout(layout.layout(), Map.of('#', menu().item("common.border", null)), null);
        return layout;
    }

    /** setItem no primeiro slot do char, se existir no layout. */
    protected void setAt(GuiLayoutLoader.GuiLayout layout, char c, ItemStack item, Consumer<InventoryClickEvent> action) {
        int slot = layout.firstSlot(c);
        if (slot >= 0) setItem(slot, item, action);
    }

    protected void setAt(GuiLayoutLoader.GuiLayout layout, char c, ItemStack item) {
        setAt(layout, c, item, null);
    }

    protected com.alkacode.fish.config.MenuConfig menu() {
        return plugin.getMenuConfig();
    }

    /** Icone de menus.yml.<path> com placeholders. */
    protected ItemStack icon(String path, Map<String, String> placeholders) {
        return menu().item(path, placeholders);
    }

    protected ItemStack icon(String path) {
        return icon(path, null);
    }

    protected ItemStack icon(String path, Map<String, String> placeholders,
                              java.util.List<net.kyori.adventure.text.Component> extraLore) {
        return menu().item(path, placeholders, extraLore);
    }

    /** Cabeca de jogador (skin real, via head()) com nome/lore de menus.yml.<path>. */
    protected ItemStack headIcon(String path, String playerName, Map<String, String> placeholders) {
        String name = menu().name(path, placeholders);
        java.util.List<String> lore = menu().rawLore(path, placeholders);
        return head(playerName, name, lore.toArray(new String[0]));
    }
}
