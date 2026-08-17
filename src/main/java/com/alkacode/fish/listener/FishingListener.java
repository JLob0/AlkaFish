package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.FishCaughtEntity;
import com.alkacode.fish.gui.FishingAreaGui;
import com.alkacode.fish.model.Bait;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.FishRarity;
import com.alkacode.fish.model.PlayerFishStats;
import com.alkacode.fish.util.WeightUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Núcleo de pesca:
 * - Dentro de uma área setada: a animação vanilla acontece normalmente e, quando a
 *   linha assenta na água dentro da área, inicia o modo AFK (FishingTask).
 * - Fora da área (global): pesca vanilla interceptada, processa o peixe na sacola.
 * - Shift + clique direito na vara abre o menu principal de pesca.
 */
public final class FishingListener implements Listener {

    private final AlkaFishPlugin plugin;

    public FishingListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        Location hookLoc = hook.getLocation();

        // Dentro da área: deixa a animação vanilla acontecer (FISHING NÃO é cancelado).
        // Quando a linha assenta na água dentro da área, inicia o modo AFK.
        if (plugin.getFishingAreaManager().isInArea(hookLoc)) {
            if (event.getState() == PlayerFishEvent.State.FISHING) {
                // Aguarda o hook assentar na água antes de iniciar o AFK
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    Location current = hook.getLocation();
                    if (plugin.getFishingAreaManager().isWaterInArea(current)) {
                        plugin.getFishingTask().start(player, current);
                    }
                }, 10L);
            } else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                if (plugin.getFishingTask().isFishing(player)) {
                    event.setCancelled(true);
                    if (event.getCaught() instanceof Item) event.getCaught().remove();
                }
            }
            return;
        }

        // Fora da área -> pesca normal
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() instanceof Item) event.getCaught().remove();
        event.setCancelled(true);
        processCatch(player, hookLoc);
    }

    /** Shift + clique direito na vara abre o menu principal de pesca. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getRodManager().isHoldingRod(player)) return;
        event.setCancelled(true);
        new FishingAreaGui(plugin, player).open();
    }

    /** Ativa uma isca quando o jogador joga a linha na água. */
    @EventHandler
    public void onBaitThrow(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) return;
        Player player = event.getPlayer();
        ItemStack bait = player.getInventory().getItemInMainHand();
        if (!plugin.getBaitManager().isBaitItem(bait)) return;
        String baitId = plugin.getBaitManager().getBaitIdFromItem(bait);
        Bait baitDef = plugin.getBaitManager().getBaitById(baitId);
        if (baitDef == null) return;
        plugin.getBaitManager().activateBait(player, baitDef, event.getHook().getLocation());
        if (bait.getAmount() > 1) bait.setAmount(bait.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);
        event.setCancelled(true);
    }

    /** Ciclo de captura do modo AFK (chamado pelo FishingTask). Retorna true se capturou. */
    public boolean afkCatch(Player player, Location hookLoc) {
        Fish fish = rollFish(player, hookLoc);
        if (fish == null) return false;
        double length = WeightUtil.rollLength(fish);
        double weight = WeightUtil.rollWeight(fish, length);
        processCaptured(player, fish, length, weight, hookLoc);
        return true;
    }

    private void processCatch(Player player, Location hookLoc) {
        Fish fish = rollFish(player, hookLoc);
        if (fish == null) return;
        double length = WeightUtil.rollLength(fish);
        double weight = WeightUtil.rollWeight(fish, length);
        processCaptured(player, fish, length, weight, hookLoc);
    }

    private Fish rollFish(Player player, Location hookLoc) {
        String biome = hookLoc.getBlock().getBiome().getKey().asString();
        World world = hookLoc.getWorld();
        boolean isNight = world.getTime() > 13000 && world.getTime() < 23000;
        boolean isRaining = world.hasStorm();
        int depth = calculateDepth(hookLoc);

        List<Fish> candidates = plugin.getFishManager().getFishForConditions(biome, isNight, isRaining, depth);
        if (candidates.isEmpty()) return null;

        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        if (stats.isRodBroken()) {
            player.sendMessage(plugin.getMessages().parse("rod.broken"));
            return null;
        }

        double luckModifier = 1.0;
        if (plugin.getBaitManager().hasActiveBait(player.getUniqueId(), hookLoc)) {
            luckModifier += plugin.getBaitManager().getBaitLuckBonus(player.getUniqueId());
        }
        if (plugin.getAlkaVipsHook() != null && plugin.getAlkaVipsHook().isAvailable()) {
            luckModifier += plugin.getAlkaVipsHook().getFishingLuckBonus(player.getUniqueId());
        }
        luckModifier += (stats.getLevel() * 0.005);
        luckModifier += plugin.getEnchantmentManager().getTotalLuckBonus(player) / 100.0;
        luckModifier += plugin.getFishingClassManager().getFishChanceBonus(player) / 100.0;

        // Booster FISH_CHANCE (+% de chance de peixe melhor)
        luckModifier += plugin.getBoosterService().getFishChance(player) / 100.0;

        return weightedRoll(candidates, luckModifier);
    }

    private Fish weightedRoll(List<Fish> candidates, double luckModifier) {
        double totalWeight = 0;
        for (Fish fish : candidates) {
            totalWeight += fish.getRarity().getBaseChance() * luckModifier;
        }
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        for (Fish fish : candidates) {
            roll -= fish.getRarity().getBaseChance() * luckModifier;
            if (roll <= 0) return fish;
        }
        return candidates.get(candidates.size() - 1);
    }

    private int calculateDepth(Location loc) {
        int y = loc.getBlockY();
        int seaLevel = loc.getWorld().getSeaLevel();
        return Math.max(0, seaLevel - y);
    }

    /** Pipeline comum de captura: sacola no banco + stats + corais + XP + torneio. */
    private void processCaptured(Player player, Fish fish, double length, double weight, Location loc) {
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        // Verificar peso suportado pela vara
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod != null && weight > rod.getSupportedWeight()) {
            if (rod.isBreakOnHeavy()) {
                stats.setRodBroken(true);
                player.sendMessage(plugin.getMessages().parse("rod.broken"));
                plugin.getPlayerDataManager().save(player.getUniqueId());
                plugin.getFishingTask().stop(player);
                return; // Peixe escapa
            }
            player.sendMessage(plugin.getMessages().parse("rod.cannot-carry"));
            return;
        }

        // Peixe vai direto para a sacola no banco (nunca para o inventário)
        plugin.getFishBagService().add(player, fish, weight);

        stats.addCatch(fish, length, weight);
        stats.addToBag(weight);

        fishCaughtRepositoryInsert(player, fish, length, weight, loc);
        plugin.getTournamentManager().registerCatch(player, fish, length, weight);

        player.sendMessage(plugin.getMessages().parse("fish.caught", java.util.Map.of(
                "fish", fish.getDisplayName(),
                "rarity", fish.getRarity().coloredName(),
                "length", String.format("%.1f", length),
                "weight", String.format("%.2f", weight))));

        if (fish.getRarity().ordinal() >= FishRarity.LEGENDARY.ordinal()) {
            Bukkit.broadcast(plugin.getMessages().parse("fish.legendary-broadcast", java.util.Map.of(
                    "player", player.getName(),
                    "fish", fish.getDisplayName(),
                    "length", String.format("%.1f", length))));
        }

        // Nacar (corais) com booster CORAL_MULTIPLIER
        double nacarReward = fish.getBasePrice() * 0.1;
        nacarReward *= (1 + plugin.getEnchantmentManager().getTotalMultiplierBonus(player));
        nacarReward *= (1 + plugin.getFishingClassManager().getCoinBonus(player) / 100.0);
        nacarReward *= (1 + plugin.getBoosterService().getCoralMultiplier(player) / 100.0);
        stats.setNacar(stats.getNacar() + nacarReward);

        plugin.getEnchantmentManager().processKeychain(player);
        processRodRewards(player, rod);

        if (plugin.getConfig().getBoolean("mcmmo.enabled", true) && plugin.getMcMMOHook() != null
                && plugin.getMcMMOHook().isAvailable()) {
            double mcmmoXp = plugin.getConfig().getDouble("mcmmo.base-xp", 5.0) * weight;
            plugin.getMcMMOHook().addFishingXp(player, mcmmoXp);
        }

        plugin.getFishingAreaManager().incrementFishCount(player);
        plugin.getLevelManager().addXp(player, stats, fish.getXpReward());
        plugin.getPlayerDataManager().save(player.getUniqueId());
    }

    private void processRodRewards(Player player, com.alkacode.fish.model.FishingRod rod) {
        if (rod == null || rod.getRewards().isEmpty()) return;
        for (com.alkacode.fish.model.FishingRod.RodReward reward : rod.getRewards()) {
            if (ThreadLocalRandom.current().nextDouble(0, 100) <= reward.chance()) {
                plugin.getRewardManager().executeReward(player, reward.rewardId());
            }
        }
    }

    private void fishCaughtRepositoryInsert(Player player, Fish fish, double length, double weight, Location loc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getFishCaughtRepository().insert(new FishCaughtEntity(
                        0, player.getUniqueId(), fish.getId(), length, weight,
                        new Timestamp(System.currentTimeMillis()),
                        loc.getBlock().getBiome().getKey().asString(), loc.getWorld().getName()));
            } catch (Exception ignored) {}
        });
    }
}
