package com.alkacode.fish.listener;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.FishCaughtEntity;
import com.alkacode.fish.model.Bait;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.FishRarity;
import com.alkacode.fish.model.PlayerFishStats;
import com.alkacode.fish.util.WeightUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Núcleo do mini-game: roll do peixe, mini-game de tensão e captura. */
public final class FishingListener implements Listener {

    private final AlkaFishPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public FishingListener(AlkaFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        Location hookLoc = event.getHook().getLocation();

        if (event.getCaught() instanceof Item) {
            event.getCaught().remove();
        }
        event.setCancelled(true);

        // Bait activation (right-click water handled elsewhere; check active zone)
        if (plugin.getConfig().getBoolean("tension-game.enabled", true)) {
            startTensionGame(player, hookLoc);
        } else {
            processCatch(player, hookLoc);
        }
    }

    private void startTensionGame(Player player, Location hookLoc) {
        Fish fish = rollFish(player, hookLoc);
        if (fish == null) return;

        double length = WeightUtil.rollLength(fish);
        double weight = WeightUtil.rollWeight(fish, length);
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        if (!stats.canAddToBag(weight)) {
            player.sendMessage(plugin.getMessages().parse("fish.bag-full"));
            return;
        }

        plugin.getTensionGameManager().startGame(player, fish, length, weight, hookLoc, (success) -> {
            if (success) {
                handleSuccessfulCatch(player, fish, length, weight, hookLoc, stats);
            } else {
                player.sendMessage(plugin.getMessages().parse("fish.escaped"));
            }
        });
    }

    private void processCatch(Player player, Location hookLoc) {
        Fish fish = rollFish(player, hookLoc);
        if (fish == null) return;
        double length = WeightUtil.rollLength(fish);
        double weight = WeightUtil.rollWeight(fish, length);
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        if (!stats.canAddToBag(weight)) {
            player.sendMessage(plugin.getMessages().parse("fish.bag-full"));
            return;
        }
        handleSuccessfulCatch(player, fish, length, weight, hookLoc, stats);
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

        // Vara quebrada impede pescar
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

        // Encantamento Lucky (bonus % -> /100)
        luckModifier += plugin.getEnchantmentManager().getTotalLuckBonus(player) / 100.0;

        // Bônus de classe
        luckModifier += plugin.getFishingClassManager().getFishChanceBonus(player) / 100.0;

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

    private void handleSuccessfulCatch(Player player, Fish fish, double length, double weight, Location loc, PlayerFishStats stats) {
        // Verificar peso suportado pela vara
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        if (rod != null && weight > rod.getSupportedWeight()) {
            if (rod.isBreakOnHeavy()) {
                stats.setRodBroken(true);
                player.sendMessage(plugin.getMessages().parse("rod.broken"));
                plugin.getPlayerDataManager().save(player.getUniqueId());
                return; // Peixe escapa
            }
            player.sendMessage(plugin.getMessages().parse("rod.cannot-carry"));
            return;
        }

        stats.addToBag(weight);
        stats.addCatch(fish, length, weight);

        ItemStack fishItem = fish.toItemStack(plugin, length, weight);
        player.getInventory().addItem(fishItem).values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));

        // Auto-sell via AlkaShop hook se habilitado
        boolean autoSold = false;
        if (plugin.getConfig().getBoolean("integrations.alkashop-sell-enabled", true)
                && plugin.getAlkaShopHook() != null && plugin.getAlkaShopHook().isAvailable()) {
            if (plugin.getAlkaShopHook().isAutoSellActive(player)) {
                var totals = plugin.getAlkaShopHook().sellItems(player, List.of(fishItem));
                if (!totals.isEmpty()) {
                    plugin.getAlkaShopHook().notifyAutoSell(player, totals);
                    stats.removeFromBag(weight);
                    autoSold = true;
                }
            }
        }

        fishCaughtRepositoryInsert(player, fish, length, weight, loc);

        plugin.getTournamentManager().registerCatch(player, fish, length, weight);

        if (!autoSold) {
            player.sendMessage(plugin.getMessages().parse("fish.caught", java.util.Map.of(
                    "fish", fish.getDisplayName(),
                    "rarity", fish.getRarity().coloredName(),
                    "length", String.format("%.1f", length),
                    "weight", String.format("%.2f", weight))));
        }

        if (fish.getRarity().ordinal() >= FishRarity.LEGENDARY.ordinal()) {
            Bukkit.broadcast(plugin.getMessages().parse("fish.legendary-broadcast", java.util.Map.of(
                    "player", player.getName(),
                    "fish", fish.getDisplayName(),
                    "length", String.format("%.1f", length))));
        }

        // Nacar (corais)
        double nacarReward = fish.getBasePrice() * 0.1;
        nacarReward *= (1 + plugin.getEnchantmentManager().getTotalMultiplierBonus(player));
        nacarReward *= (1 + plugin.getFishingClassManager().getCoinBonus(player) / 100.0);
        stats.setNacar(stats.getNacar() + nacarReward);

        // Keychain
        plugin.getEnchantmentManager().processKeychain(player);

        // Recompensas da vara
        processRodRewards(player, rod);

        // mcMMO XP
        if (plugin.getConfig().getBoolean("mcmmo.enabled", true) && plugin.getMcMMOHook() != null
                && plugin.getMcMMOHook().isAvailable()) {
            double mcmmoXp = plugin.getConfig().getDouble("mcmmo.base-xp", 5.0) * weight;
            plugin.getMcMMOHook().addFishingXp(player, mcmmoXp);
        }

        // Contador de peixes na área
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
}
