package com.alkacode.fish.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.gui.layout.GuiLayoutLoader;
import org.bukkit.Material;
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
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black>♥");
        fillBorder(glass);
    }

    /** Aplica o layout do YML: preenche a borda (#) com o mesmo painel preto do fillBlack()
     * e retorna o layout, pra depois usar setAt(...) nos chars de conteudo. */
    protected GuiLayoutLoader.GuiLayout applyBorder(String layoutName) {
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout(layoutName);
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black>♥");
        layout(layout.layout(), Map.of('#', glass), null);
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
}
