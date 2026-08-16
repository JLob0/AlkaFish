package com.alkacode.fish;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.fish.api.AlkaFishAPI;
import com.alkacode.fish.command.AlkaFishAdminCommand;
import com.alkacode.fish.command.FishCommand;
import com.alkacode.fish.command.SairCommand;
import com.alkacode.fish.database.repository.FishCaughtRepository;
import com.alkacode.fish.database.repository.PlayerFishDataRepository;
import com.alkacode.fish.database.repository.TournamentRecordRepository;
import com.alkacode.fish.gui.CodexGui;
import com.alkacode.fish.gui.FishBagGui;
import com.alkacode.fish.gui.SellGui;
import com.alkacode.fish.gui.TournamentGui;
import com.alkacode.fish.hooks.AlkaClansHook;
import com.alkacode.fish.hooks.AlkaDropHook;
import com.alkacode.fish.hooks.AlkaEconomyBridge;
import com.alkacode.fish.hooks.AlkaRankUpHook;
import com.alkacode.fish.hooks.AlkaShopHook;
import com.alkacode.fish.hooks.AlkaTimeHook;
import com.alkacode.fish.hooks.AlkaVipsHook;
import com.alkacode.fish.hooks.ItemsAdderHook;
import com.alkacode.fish.hooks.LuckPermsHook;
import com.alkacode.fish.hooks.McMMOHook;
import com.alkacode.fish.hooks.WorldEditHook;
import com.alkacode.fish.hooks.WorldGuardHook;
import com.alkacode.fish.listener.CookingListener;
import com.alkacode.fish.listener.FishingAreaTrackerListener;
import com.alkacode.fish.listener.FishingListener;
import com.alkacode.fish.listener.PlayerJoinQuitListener;
import com.alkacode.fish.manager.BaitManager;
import com.alkacode.fish.manager.CookingManager;
import com.alkacode.fish.manager.EnchantmentManager;
import com.alkacode.fish.manager.FishingAreaManager;
import com.alkacode.fish.manager.FishingClassManager;
import com.alkacode.fish.manager.FishManager;
import com.alkacode.fish.manager.LevelManager;
import com.alkacode.fish.manager.NpcManager;
import com.alkacode.fish.manager.PlayerDataManager;
import com.alkacode.fish.manager.RewardManager;
import com.alkacode.fish.manager.RodManager;
import com.alkacode.fish.manager.TensionGameManager;
import com.alkacode.fish.manager.TournamentManager;
import com.alkacode.fish.placeholder.AlkaFishExpansion;
import org.bukkit.Bukkit;

import java.util.logging.Level;

/** AlkaFish - sistema de pesca da rede AlkaStudio. */
public final class AlkaFishPlugin extends AlkaPlugin {

    private static AlkaFishPlugin instance;

    private FishMessages messages;
    private AlkaFishAPI api;

    private FishManager fishManager;
    private PlayerDataManager playerDataManager;
    private TournamentManager tournamentManager;
    private BaitManager baitManager;
    private CookingManager cookingManager;
    private TensionGameManager tensionGameManager;
    private LevelManager levelManager;
    private RodManager rodManager;
    private EnchantmentManager enchantmentManager;
    private FishingClassManager fishingClassManager;
    private NpcManager npcManager;
    private FishingAreaManager fishingAreaManager;
    private RewardManager rewardManager;

    private PlayerFishDataRepository playerFishDataRepository;
    private FishCaughtRepository fishCaughtRepository;
    private TournamentRecordRepository tournamentRecordRepository;

    private AlkaEconomyBridge economyBridge;

    private AlkaShopHook alkaShopHook;
    private AlkaDropHook alkaDropHook;
    private AlkaRankUpHook alkaRankUpHook;
    private AlkaVipsHook alkaVipsHook;
    private AlkaClansHook alkaClansHook;
    private AlkaTimeHook alkaTimeHook;
    private ItemsAdderHook itemsAdderHook;
    private LuckPermsHook luckPermsHook;
    private WorldGuardHook worldGuardHook;
    private McMMOHook mcMMOHook;
    private WorldEditHook worldEditHook;

    @Override
    protected void onPluginEnable() {
        instance = this;
        AlkaAPI core = getAlkaAPI();

        saveDefaultConfig();
        saveResource("fish.yml", false);
        saveResource("baits.yml", false);
        saveResource("tournaments.yml", false);
        saveResource("cooking.yml", false);
        saveResource("messages.yml", false);
        saveResource("rods.yml", false);
        saveResource("enchantments.yml", false);
        saveResource("classes.yml", false);
        saveResource("npc.yml", false);
        saveResource("rewards.yml", false);
        saveResource("fishingarea.yml", false);

        this.messages = new FishMessages(this);

        AlkaAPI apiCore = getAlkaAPI();
        this.playerFishDataRepository = new PlayerFishDataRepository(apiCore.getDatabase());
        this.fishCaughtRepository = new FishCaughtRepository(apiCore.getDatabase());
        this.tournamentRecordRepository = new TournamentRecordRepository(apiCore.getDatabase());
        createTables();

        this.economyBridge = new AlkaEconomyBridge();

        this.fishManager = new FishManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.baitManager = new BaitManager(this);
        this.cookingManager = new CookingManager(this);
        this.tensionGameManager = new TensionGameManager(this);
        this.levelManager = new LevelManager(this);
        this.tournamentManager = new TournamentManager(this);
        this.rodManager = new RodManager(this);
        this.enchantmentManager = new EnchantmentManager(this);
        this.fishingClassManager = new FishingClassManager(this);
        this.npcManager = new NpcManager(this);
        this.fishingAreaManager = new FishingAreaManager(this);
        this.rewardManager = new RewardManager(this);

        this.api = new AlkaFishAPI(this);

        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new CookingListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingAreaTrackerListener(this), this);

        getCommand("fish").setExecutor(new FishCommand(this));
        getCommand("sair").setExecutor(new SairCommand(this));
        getCommand("alkafish").setExecutor(new AlkaFishAdminCommand(this));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AlkaFishExpansion(this).register();
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            this.alkaShopHook = new AlkaShopHook(this);
            this.alkaDropHook = new AlkaDropHook(this);
            this.alkaRankUpHook = new AlkaRankUpHook(this);
            this.alkaVipsHook = new AlkaVipsHook(this);
            this.alkaClansHook = new AlkaClansHook(this);
            this.alkaTimeHook = new AlkaTimeHook(this);
            this.itemsAdderHook = new ItemsAdderHook(this);
            this.luckPermsHook = new LuckPermsHook(this);
            this.worldGuardHook = new WorldGuardHook(this);
            this.mcMMOHook = new McMMOHook(this);
            this.worldEditHook = new WorldEditHook();
            getLogger().info("Hooks lazy-initialized.");
        }, 1L);

        getServer().getScheduler().runTaskAsynchronously(this, () -> playerDataManager.loadAllOnline());

        getLogger().info("AlkaFish v" + getDescription().getVersion() + " habilitado.");
    }

    private void createTables() {
        try {
            playerFishDataRepository.createTable();
            fishCaughtRepository.createTable();
            tournamentRecordRepository.createTable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Falha ao criar tabelas do AlkaFish", e);
        }
    }

    @Override
    protected void onPluginDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
        }
        if (tournamentManager != null) {
            tournamentManager.shutdown();
        }
        if (fishingAreaManager != null) {
            fishingAreaManager.shutdown();
        }
        if (npcManager != null) {
            npcManager.removeNpc();
        }
        getAlkaAPI().getScheduler().cancelAll();
        instance = null;
    }

    public static AlkaFishPlugin getInstance() { return instance; }

    public FishMessages getMessages() { return messages; }
    public AlkaFishAPI getApi() { return api; }

    public FishManager getFishManager() { return fishManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public TournamentManager getTournamentManager() { return tournamentManager; }
    public BaitManager getBaitManager() { return baitManager; }
    public CookingManager getCookingManager() { return cookingManager; }
    public TensionGameManager getTensionGameManager() { return tensionGameManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public RodManager getRodManager() { return rodManager; }
    public EnchantmentManager getEnchantmentManager() { return enchantmentManager; }
    public FishingClassManager getFishingClassManager() { return fishingClassManager; }
    public NpcManager getNpcManager() { return npcManager; }
    public FishingAreaManager getFishingAreaManager() { return fishingAreaManager; }
    public RewardManager getRewardManager() { return rewardManager; }

    public PlayerFishDataRepository getPlayerFishDataRepository() { return playerFishDataRepository; }
    public FishCaughtRepository getFishCaughtRepository() { return fishCaughtRepository; }
    public TournamentRecordRepository getTournamentRecordRepository() { return tournamentRecordRepository; }

    public AlkaEconomyBridge getEconomyBridge() { return economyBridge; }

    public AlkaShopHook getAlkaShopHook() { return alkaShopHook; }
    public AlkaDropHook getAlkaDropHook() { return alkaDropHook; }
    public AlkaRankUpHook getAlkaRankUpHook() { return alkaRankUpHook; }
    public AlkaVipsHook getAlkaVipsHook() { return alkaVipsHook; }
    public AlkaClansHook getAlkaClansHook() { return alkaClansHook; }
    public AlkaTimeHook getAlkaTimeHook() { return alkaTimeHook; }
    public ItemsAdderHook getItemsAdderHook() { return itemsAdderHook; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public McMMOHook getMcMMOHook() { return mcMMOHook; }
    public WorldEditHook getWorldEditHook() { return worldEditHook; }
}
