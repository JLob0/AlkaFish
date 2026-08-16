package com.alkacode.fish.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Estado de pesca de um jogador (nível, XP, estatísticas e peso da bag). */
public class PlayerFishStats {
    private final UUID playerUuid;
    private int level;
    private double xp;
    private int totalCaught;
    private double biggestLength;
    private String biggestFishId;
    private double totalWeight;
    private final Map<String, Integer> caughtByFishId;
    private double bagCapacity;
    private double currentBagWeight;

    // === NOVOS CAMPOS v1.1.0 ===
    private String rodId = "wooden";
    private int rodLevel = 1;
    private boolean rodBroken = false;
    private final Map<String, Integer> rodEnchantLevels = new HashMap<>();
    private String activeClassId = "";
    private final Set<String> unlockedClasses = new HashSet<>();
    private double nacar = 0;
    private double nacarNext = 100;

    public PlayerFishStats(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.level = 1;
        this.xp = 0;
        this.totalCaught = 0;
        this.biggestLength = 0;
        this.biggestFishId = "";
        this.totalWeight = 0;
        this.caughtByFishId = new HashMap<>();
        this.bagCapacity = 50.0;
        this.currentBagWeight = 0;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getXp() { return xp; }
    public void setXp(double xp) { this.xp = xp; }
    public int getTotalCaught() { return totalCaught; }
    public void setTotalCaught(int totalCaught) { this.totalCaught = totalCaught; }
    public double getBiggestLength() { return biggestLength; }
    public void setBiggestLength(double biggestLength) { this.biggestLength = biggestLength; }
    public String getBiggestFishId() { return biggestFishId; }
    public void setBiggestFishId(String biggestFishId) { this.biggestFishId = biggestFishId; }
    public double getTotalWeight() { return totalWeight; }
    public void setTotalWeight(double totalWeight) { this.totalWeight = totalWeight; }
    public Map<String, Integer> getCaughtByFishId() { return caughtByFishId; }
    public double getBagCapacity() { return bagCapacity; }
    public void setBagCapacity(double bagCapacity) { this.bagCapacity = bagCapacity; }
    public double getCurrentBagWeight() { return currentBagWeight; }
    public void setCurrentBagWeight(double currentBagWeight) { this.currentBagWeight = currentBagWeight; }

    public String getRodId() { return rodId; }
    public void setRodId(String rodId) { this.rodId = rodId; }
    public int getRodLevel() { return rodLevel; }
    public void setRodLevel(int rodLevel) { this.rodLevel = rodLevel; }
    public boolean isRodBroken() { return rodBroken; }
    public void setRodBroken(boolean rodBroken) { this.rodBroken = rodBroken; }
    public Map<String, Integer> getRodEnchantLevels() { return rodEnchantLevels; }
    public int getRodEnchantLevel(String enchantId) { return rodEnchantLevels.getOrDefault(enchantId, 0); }
    public void setRodEnchantLevel(String enchantId, int level) { rodEnchantLevels.put(enchantId, level); }
    public String getActiveClassId() { return activeClassId; }
    public void setActiveClassId(String activeClassId) { this.activeClassId = activeClassId; }
    public Set<String> getUnlockedClasses() { return unlockedClasses; }
    public double getNacar() { return nacar; }
    public void setNacar(double nacar) { this.nacar = nacar; }
    public double getNacarNext() { return nacarNext; }
    public void setNacarNext(double nacarNext) { this.nacarNext = nacarNext; }

    public void addCatch(Fish fish, double length, double weight) {
        totalCaught++;
        totalWeight += weight;
        caughtByFishId.merge(fish.getId(), 1, Integer::sum);
        if (length > biggestLength) {
            biggestLength = length;
            biggestFishId = fish.getId();
        }
        xp += fish.getXpReward();
    }

    public static double getXpForLevel(int level) {
        return 100 * Math.pow(level, 1.5);
    }

    public boolean canAddToBag(double weight) {
        return currentBagWeight + weight <= bagCapacity;
    }

    public void addToBag(double weight) {
        this.currentBagWeight += weight;
    }

    public void removeFromBag(double weight) {
        this.currentBagWeight = Math.max(0, this.currentBagWeight - weight);
    }
}
