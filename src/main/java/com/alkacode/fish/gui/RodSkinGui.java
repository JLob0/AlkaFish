package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Sub-menu de skins da vara: troca só a textura (custom-model-data do ItemsAdder) entre
 * as varas que o jogador já passou na progressão, sem mexer no nível/stats reais. */
public final class RodSkinGui extends FishGui {

    private static final int PER_PAGE = 6; // slots 11-16

    private final int page;

    public RodSkinGui(AlkaFishPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public RodSkinGui(AlkaFishPlugin plugin, Player player, int page) {
        super(plugin, player, "🎨 Skins da Vara", Category.PROGRESSION, 3, "alkafish-rod-skin");
        this.page = page;
    }

    @Override
    public void render() {
        var layout = applyBorder("rod-skin");
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod current = plugin.getRodManager().getRodById(stats.getRodId());
        if (current == null) current = plugin.getRodManager().getDefaultRod();
        if (current == null) return;
        final FishingRod fCurrent = current;

        String activeSkin = stats.getRodSkinId();
        boolean usingDefault = activeSkin == null || activeSkin.isEmpty();

        // S: reset pra skin própria da vara atual (fixo em toda página)
        setAt(layout, 'S', createItem(usingDefault ? Material.LIME_DYE : Material.GRAY_DYE,
                usingDefault ? "<green>✔ Skin Padrão" : "<gray>Skin Padrão",
                "<gray>Usa a textura da sua vara atual:",
                "<white>" + fCurrent.getDisplayName(),
                "",
                usingDefault ? "<green>Selecionada" : "<yellow>Clique para usar"), e -> {
            plugin.getRodManager().setRodSkin(player, "");
            refresh();
        });

        List<FishingRod> options = new ArrayList<>();
        for (FishingRod rod : plugin.getRodManager().getUnlockedRods(fCurrent)) {
            if (!rod.getId().equals(fCurrent.getId())) options.add(rod);
        }

        int totalPages = Math.max(1, (int) Math.ceil(options.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, options.size());

        var contentSlots = layout.findSlots('0');
        int idx = 0;
        for (FishingRod rod : options.subList(from, to)) {
            if (idx >= contentSlots.size()) break;
            boolean selected = rod.getId().equals(activeSkin);
            final String rodId = rod.getId();
            int slot = contentSlots.get(idx++);
            setItem(slot, createItem(selected ? Material.LIME_DYE : Material.CYAN_DYE,
                    (selected ? "<green>✔ " : "<aqua>") + rod.getDisplayName(),
                    "<gray>Nível: <aqua>" + rod.getLevel(),
                    "",
                    selected ? "<green>Selecionada" : "<yellow>Clique para usar essa skin"), e -> {
                plugin.getRodManager().setRodSkin(player, rodId);
                refresh();
            });
        }

        if (safePage > 0) {
            setAt(layout, 'P', createItem(Material.ARROW, "<yellow>◀ Página Anterior"),
                    e -> new RodSkinGui(plugin, player, safePage - 1).open());
        }
        if (safePage < totalPages - 1) {
            setAt(layout, 'N', createItem(Material.ARROW, "<yellow>Próxima Página ▶"),
                    e -> new RodSkinGui(plugin, player, safePage + 1).open());
        }

        setAt(layout, 'V', createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new RodGui(plugin, player).open());
        setAt(layout, 'F', createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }
}
