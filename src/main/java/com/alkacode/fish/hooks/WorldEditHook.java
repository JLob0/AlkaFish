package com.alkacode.fish.hooks;

import com.alkacode.fish.model.FishingRegion;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;

import java.util.Optional;

/** Captura a seleção ativa do jogador no WorldEdit/FAWE (//pos1, //pos2, //wand). */
public class WorldEditHook {

    public Optional<FishingRegion> getSelection(Player player) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return Optional.of(new FishingRegion(
                    player.getWorld().getName(),
                    min.getX(), min.getY(), min.getZ(),
                    max.getX(), max.getY(), max.getZ()));
        } catch (IncompleteRegionException | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
}
