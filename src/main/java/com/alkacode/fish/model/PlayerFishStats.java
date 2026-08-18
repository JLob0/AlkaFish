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
    /** Limite de ITENS (soma de amount) que a Fish Bag aguenta - upgradável com coins
     * (ver FishBagService#upgradeBagLimit). Não é peso: peixe lendário pesa milhares de
     * kg, um limite em peso travaria ele pra sempre. */
    private double bagCapacity;
    private double currentBagWeight;

    // === NOVOS CAMPOS v1.1.0 ===
    private String rodId = "quebravel";
    private int rodLevel = 0;
    private boolean rodBroken = false;
    private final Map<String, Integer> rodEnchantLevels = new HashMap<>();
    private String activeClassId = "";
    private final Set<String> unlockedClasses = new HashSet<>();
    /** Nacar pescado com a vara ATUAL (não é saldo - saldo real fica na AlkaEconomy).
     * Zera a cada upgrade de vara - mostra o "quanto essa vara já rendeu". */
    private double rodNacarEarned = 0;
    private double nacarNext = 100;
    private boolean autoUpgradeEnabled = false;
    /** Id da vara escolhida como skin visual (item custom do ItemsAdder) - vazio = usa a
     * skin da própria vara atual. Só pode apontar pra uma vara de nível <= rodLevel (já
     * desbloqueada); as stats reais (peso/delay/quebra) sempre vêm da vara real, nunca da skin. */
    private String rodSkinId = "";
    /** Perk VIP: vende a sacola inteira sozinha assim que encher (em vez de escapar o
     * peixe/parar a pesca) - gated pelo Perk Tree do AlkaVips, mesmo padrão do auto-upgrade. */
    private boolean autoSellOnFull = false;
    /** Inventário salvo ao entrar na área de pesca (InventoryCodec) - persistido no banco
     * pra sobreviver um restart do servidor com o jogador dentro. Vazio = nada salvo. */
    private String savedInventory = "";
    /** Tempo total (lifetime, em segundos) que o jogador já passou pescando - pro TOP de tempo. */
    private long totalFishingSeconds = 0;

    public PlayerFishStats(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.level = 1;
        this.xp = 0;
        this.totalCaught = 0;
        this.biggestLength = 0;
        this.biggestFishId = "";
        this.totalWeight = 0;
        this.caughtByFishId = new HashMap<>();
        this.bagCapacity = 500.0;
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
    public double getRodNacarEarned() { return rodNacarEarned; }
    public void setRodNacarEarned(double rodNacarEarned) { this.rodNacarEarned = rodNacarEarned; }
    public double getNacarNext() { return nacarNext; }
    public void setNacarNext(double nacarNext) { this.nacarNext = nacarNext; }
    public boolean isAutoUpgradeEnabled() { return autoUpgradeEnabled; }
    public void setAutoUpgradeEnabled(boolean autoUpgradeEnabled) { this.autoUpgradeEnabled = autoUpgradeEnabled; }
    public String getRodSkinId() { return rodSkinId; }
    public void setRodSkinId(String rodSkinId) { this.rodSkinId = rodSkinId == null ? "" : rodSkinId; }
    public boolean isAutoSellOnFull() { return autoSellOnFull; }
    public void setAutoSellOnFull(boolean autoSellOnFull) { this.autoSellOnFull = autoSellOnFull; }
    public String getSavedInventory() { return savedInventory; }
    public void setSavedInventory(String savedInventory) { this.savedInventory = savedInventory == null ? "" : savedInventory; }
    public long getTotalFishingSeconds() { return totalFishingSeconds; }
    public void setTotalFishingSeconds(long totalFishingSeconds) { this.totalFishingSeconds = totalFishingSeconds; }

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
