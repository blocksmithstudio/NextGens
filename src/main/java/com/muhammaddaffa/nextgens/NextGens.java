package com.muhammaddaffa.nextgens;

import com.bgsoftware.wildtools.api.WildToolsAPI;
import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.configupdater.ConfigUpdater;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckSource;
import com.muhammaddaffa.mdlib.updatechecker.UpdateChecker;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.api.GeneratorAPI;
import com.muhammaddaffa.nextgens.autosell.AutosellManager;
import com.muhammaddaffa.nextgens.commands.*;
import com.muhammaddaffa.nextgens.database.ChunkCoord;
import com.muhammaddaffa.nextgens.database.DatabaseManager;
import com.muhammaddaffa.nextgens.database.listeners.GeneratorLoadChunkListener;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.listeners.*;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.generators.runnables.CorruptionTask;
import com.muhammaddaffa.nextgens.generators.runnables.GeneratorTask;
import com.muhammaddaffa.nextgens.generators.runnables.NotifyTask;
import com.muhammaddaffa.nextgens.hooks.axboosters.AxBoosterLoad;
import com.muhammaddaffa.nextgens.hooks.bento.BentoListener;
import com.muhammaddaffa.nextgens.hooks.fabledsb.FabledSbListener;
import com.muhammaddaffa.nextgens.hooks.papi.GensExpansion;
import com.muhammaddaffa.nextgens.hooks.ssb2.SSB2Listener;
import com.muhammaddaffa.nextgens.sell.listeners.HandSellListener;
import com.muhammaddaffa.nextgens.sell.multipliers.SellMultiplierRegistry;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import com.muhammaddaffa.nextgens.refund.listeners.RefundListener;
import com.muhammaddaffa.nextgens.sell.SellManager;
import com.muhammaddaffa.nextgens.sellwand.listeners.SellwandListener;
import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.UserRepository;
import com.muhammaddaffa.nextgens.utils.Settings;
import com.muhammaddaffa.nextgens.worth.WorthManager;
import dev.norska.dsw.DeluxeSellwands;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltAPI;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NextGens extends JavaPlugin {

    public static final Set<ChunkCoord> LOADED_CHUNKS = ConcurrentHashMap.newKeySet();

    private static final int BSTATS_ID = 19417;
    private static final int SPIGOT_ID = 111857;
    private static final int BUILTBYBIT_ID = 30903;

    private static NextGens instance;
    private static GeneratorAPI api;

    // -----------------------------
    // NamespacedKey Section
    public static NamespacedKey generator_id;
    public static NamespacedKey drop_value;
    public static NamespacedKey sellwand_global;
    public static NamespacedKey sellwand_multiplier;
    public static NamespacedKey sellwand_uses;
    public static NamespacedKey sellwand_total_sold;
    public static NamespacedKey sellwand_total_items;

    // End of NamespacedKey Section
    // ------------------------------

    private final DatabaseManager dbm = new DatabaseManager();
    private final EventManager eventManager = new EventManager();
    private final WorthManager worthManager = new WorthManager();
    private final GeneratorManager generatorManager = new GeneratorManager(dbm);
    private final UserManager userManager = new UserManager();
    private final UserRepository userRepository = new UserRepository(dbm, userManager);
    private final SellManager sellManager = new SellManager(userManager, eventManager);
    private final RefundManager refundManager = new RefundManager(generatorManager);
    private final SellwandManager sellwandManager = new SellwandManager();
    private final AutosellManager autosellManager = new AutosellManager(userManager);
    private final SellMultiplierRegistry sellMultiplierRegistry = new SellMultiplierRegistry();

    // API
    private BoltAPI boltAPI;
    // private AxBoostersWrapper boosterHook = player -> 1.0f;

    public static Config DEFAULT_CONFIG, GENERATORS_CONFIG, SHOP_CONFIG, UPGRADE_GUI_CONFIG, CORRUPT_GUI_CONFIG, EVENTS_CONFIG, DATA_CONFIG,
            WORTH_CONFIG, SETTINGS_GUI_CONFIG, VIEW_GUI_CONFIG;

    public static boolean STOPPING = false;

    @Override
    public void onLoad() {
        MDLib.inject(this);
    }

    @Override
    public void onEnable() {
        MDLib.onEnable(this);
        // --------------------------------------------
        instance = this;

        PluginManager pm = Bukkit.getPluginManager();
        // AxBoosters intergration
        if (pm.isPluginEnabled("AxBoosters")) {
            Logger.info("Found AxBoosters, registering hook...");
            pm.registerEvents(new AxBoosterLoad(this), this);
        }

        // fancy big text
        Logger.info("""
                NextGens plugin by aglerr - starting...
                
                ███╗░░██╗███████╗██╗░░██╗████████╗░██████╗░███████╗███╗░░██╗░██████╗
                ████╗░██║██╔════╝╚██╗██╔╝╚══██╔══╝██╔════╝░██╔════╝████╗░██║██╔════╝
                ██╔██╗██║█████╗░░░╚███╔╝░░░░██║░░░██║░░██╗░█████╗░░██╔██╗██║╚█████╗░
                ██║╚████║██╔══╝░░░██╔██╗░░░░██║░░░██║░░╚██╗██╔══╝░░██║╚████║░╚═══██╗
                ██║░╚███║███████╗██╔╝╚██╗░░░██║░░░╚██████╔╝███████╗██║░╚███║██████╔╝
                ╚═╝░░╚══╝╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░╚═════╝░╚══════╝╚═╝░░╚══╝╚═════╝░
                """);

        // initialize keys
        keys();

        // initialize configs and update
        configs();
        update();

        // initialize settings
        Settings.init();

        // connect to database and create the table
        this.dbm.connect();
        this.dbm.createGeneratorTable();
        this.dbm.createUserTable();

        // register commands & listeners
        commands();
        listeners();

        // register hook
        hooks();
        // register task
        tasks();

        // load all generators
        this.generatorManager.loadGenerators();

        Executor.asyncLater(1, () -> {
            // load chunk coords for active generators
            this.generatorManager.loadChunkCoords();
            // load users
            this.userRepository.loadUsers();

            // load the refund
            this.refundManager.load();

            // load events
            this.eventManager.loadEvents();
            this.eventManager.load();
            this.eventManager.startTask();

            // worth system
            this.worthManager.load();

            // update checker
            updateCheck();
        });

        // Check all loaded chunks on all worlds
        Executor.syncLater(20, () -> {
            for (World w : Bukkit.getWorlds()) {
                for (Chunk chunk : w.getLoadedChunks()) {
                    ChunkCoord key = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                    // If the chunk is already loaded, skip this
                    if (!NextGens.LOADED_CHUNKS.add(key)) continue;
                    // If there's no generators on this chunk, skip this
                    List<String> list = this.generatorManager.getGeneratorsByChunk().get(key);
                    if (list == null || list.isEmpty()) continue;

                    // Proceed to load the active generators
                    Executor.async(() -> this.generatorManager.loadActiveGenerator(key, list));
                }
            }
        });

        // We should reload the generators
        Executor.syncLater(40L, () -> {
            // load back the generators
            this.generatorManager.loadGenerators();
            // refresh the active generator
            Executor.async(this.generatorManager::refreshActiveGenerator);
        });
    }

    @Override
    public void onDisable() {
        STOPPING = true;
        // shutdown the lib
        MDLib.shutdown();
        // remove all holograms
        GeneratorTask.flush();
        // save all other things
        save();
        // close the database
        this.dbm.close();
    }

    private void save() {
        // save small things first
        this.refundManager.saveAll();
        this.eventManager.save();
    }

    private void keys() {
        generator_id = new NamespacedKey(this, "nextgens_generator_id");
        drop_value = new NamespacedKey(this, "nextgens_drop_value");
        sellwand_global = new NamespacedKey(this, "nextgens_sellwand_global");
        sellwand_multiplier = new NamespacedKey(this, "nextgens_sellwand_multiplier");
        sellwand_uses = new NamespacedKey(this, "nextgens_sellwand_uses");
        sellwand_total_sold = new NamespacedKey(this, "nextgens_sellwand_total_sold");
        sellwand_total_items = new NamespacedKey(this, "nextgens_sellwand_total_items");
    }

    private void tasks() {
        // start generator task
        GeneratorTask.start(this.generatorManager, this.eventManager, this.userManager);
        // corruption task
        CorruptionTask.start(this.generatorManager);
        // notify task
        NotifyTask.start(this.generatorManager);
        // autosell task
        this.autosellManager.startTask();
    }

    private void hooks() {
        PluginManager pm = Bukkit.getPluginManager();
        // papi hook
        if (pm.getPlugin("PlaceholderAPI") != null) {
            Logger.info("Found PlaceholderAPI! Registering hook...");
            new GensExpansion(this.generatorManager, this.userManager, this.eventManager, this.sellMultiplierRegistry).register();
        }
        if (pm.getPlugin("SuperiorSkyblock2") != null) {
            Logger.info("Found SuperiorSkyblock2! Registering hook...");
            pm.registerEvents(new SSB2Listener(this.generatorManager, this.refundManager), this);
        }
        if (pm.getPlugin("BentoBox") != null) {
            Logger.info("Found BentoBox! Registering hook...");
            pm.registerEvents(new BentoListener(this.generatorManager, this.refundManager), this);
        }
        if (pm.getPlugin("HolographicDisplays") != null) {
            Logger.info("Found HolographicDisplays! Registering hook...");
        }
        if (pm.getPlugin("DecentHolograms") != null) {
            Logger.info("Found DecentHolograms! Registering hook...");
        }
        if (pm.getPlugin("WildTools") != null && NextGens.DEFAULT_CONFIG.getBoolean("sellwand.hooks.wildtools")) {
            Logger.info("Found WildTools! Registering hook...");
            WildToolsAPI.getWildTools().getProviders().setPricesProvider((player, stack) -> api.getWorth(stack));
        }
        if (pm.getPlugin("DeluxeSellwands") != null) {
            Logger.info("Found DeluxeSellwands! Registering hook...");
            DeluxeSellwands.getInstance().getPriceHandler().registerNewPriceHandler("NEXTGENS", (player, itemStack, i) -> {
                double value = api.getWorth(itemStack);
                return value <= 0 ? null :value;
            });
        }
        if (pm.getPlugin("LWC") != null) {
            Logger.info("Found LWC! Registering hook...");
        }
        if (pm.getPlugin("Bolt") != null) {
            Logger.info("Found Bolt! Registering hook...");
            this.boltAPI = Bukkit.getServicesManager().load(BoltAPI.class);
        }
        if (pm.getPlugin("FabledSkyblock") != null) {
            Logger.info("Found FabledSkyblock! Registering hook...");
            pm.registerEvents(new FabledSbListener(this.generatorManager, this.refundManager), this);
        }
        // Slimefun integration
        if (pm.isPluginEnabled("Slimfun")) {
            Logger.info("Found Slimefun, registering hook...");
            pm.registerEvents(new GeneratorSlimefunPreventionListener(this.generatorManager), this);
        }
        // register bstats metrics hook
        this.connectMetrics();
    }

    private void update() {
        // check for auto config update
        if (!NextGens.DEFAULT_CONFIG.getConfig().getBoolean("auto-config-update", true)) {
            return;
        }

        File configFile = new File(this.getDataFolder(), "config.yml");
        File eventsFile = new File(this.getDataFolder(), "events.yml");

        try {
            ConfigUpdater.update(this, "config.yml", configFile, List.of("sell-options.sound"));
            ConfigUpdater.update(this, "events.yml", eventsFile, List.of("events.events"));
        } catch (IOException ex) {
            Logger.severe("Failed to update the config.yml!");
            ex.printStackTrace();
        }
        // reload the config afterward
        Config.reload();
    }

    private void configs() {
        DEFAULT_CONFIG          = new Config("config.yml", null, true);
        GENERATORS_CONFIG       = new Config("generators.yml", null, true);
        SHOP_CONFIG             = new Config("shop.yml", null, true);
        UPGRADE_GUI_CONFIG      = new Config("upgrade_gui.yml", "gui", true);
        CORRUPT_GUI_CONFIG      = new Config("corrupt_gui.yml", "gui", true);
        EVENTS_CONFIG           = new Config("events.yml", null, true);
        DATA_CONFIG             = new Config("data.yml", null, false);
        WORTH_CONFIG            = new Config("worth.yml", null, true);
        SETTINGS_GUI_CONFIG     = new Config("settings_gui.yml", "gui", true);
        VIEW_GUI_CONFIG         = new Config("view_gui.yml", "gui", true);
    }

    private void listeners() {
        PluginManager pm = Bukkit.getPluginManager();
        // generators
        pm.registerEvents(new GeneratorBreakListener(this.generatorManager, this.userManager), this);
        pm.registerEvents(new GeneratorPlaceListener(this.generatorManager, this.userManager), this);
        pm.registerEvents(new GeneratorPreventionListener(this.generatorManager), this);
        pm.registerEvents(new GeneratorUpgradeListener(this.generatorManager, this.userManager), this);
        pm.registerEvents(new GeneratorLoadChunkListener(this.generatorManager), this);
        // first join
        pm.registerEvents(new PlayerJoinListener(this.generatorManager), this);
        // sellwand
        pm.registerEvents(new SellwandListener(this.sellwandManager), this);
        // handsell
        pm.registerEvents(new HandSellListener(this.sellManager), this);
        // refund
        pm.registerEvents(new RefundListener(this.refundManager), this);
        // multipliers
        pm.registerEvents(new GeneratorWorldDropMultiplier(), this);
    }

    private void commands() {
        // register commands
        MainCommand.registerCommand(this.generatorManager, this.userManager, this.eventManager, this.worthManager, this.sellwandManager);
        PickupCommand.registerCommand(this.generatorManager);
        PlayerSettingsCommand.registerCommand(this.userManager);
        RepairGensCommand.registerCommand(this.generatorManager);
        SellCommand.registerCommand();
        ShopCommand.registerCommand(this.generatorManager);
        WorthCommand.registerCommand();
    }

    private void connectMetrics() {
        // connect to bstats metrics
        Metrics metrics = new Metrics(this, BSTATS_ID);
        // add custom charts
        Map<String, String> data = new HashMap<>();
        data.put("corruption", "corruption.enabled");
        data.put("auto_save", "auto-save.enabled");
        data.put("place_permission", "place-permission");
        data.put("online_only", "online-only");
        data.put("anti_explosion", "anti-explosion");
        data.put("disable_drop_place", "disable-drop-place");
        data.put("shift_pickup", "shift-pickup");
        data.put("island_pickup", "island-pickup");
        data.put("upgrade_gui", "upgrade-gui");
        data.put("close_on_purchase", "close-on-purchase");
        data.put("close_on_no_money", "close-on-no-money");
        data.put("hook_shopguiplus", "sell-options.hook_shopguiplus");
        data.put("drop_on_break", "drop-on-break");
        data.put("broken_pickup", "broken-pickup");
        // create a custom charts
        data.forEach((id, path) -> {
            metrics.addCustomChart(this.createSimplePie(id, path));
        });
        // single line chart
        metrics.addCustomChart(new SingleLineChart("total_generators", () -> this.generatorManager.getActiveGenerator().size()));
    }

    private SimplePie createSimplePie(String id, String path) {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        return new SimplePie(id, () -> this.yesOrNo(config.getBoolean(path)));
    }

    private void updateCheck(){
        Executor.async(() -> new UpdateChecker(this, UpdateCheckSource.SPIGOT, SPIGOT_ID + "")
                .setDownloadLink(SPIGOT_ID)
                .checkEveryXHours(24)
                .setNotifyOpsOnJoin(true)
                .setNotifyByPermissionOnJoin("nextgens.notifyupdate")
                .checkNow());
    }

    private String yesOrNo(boolean status) {
        if (status) {
            return "yes";
        } else {
            return "no";
        }
    }

    public static GeneratorAPI getApi() {
        // initialize the api
        if (api == null) {
            NextGens plugin = NextGens.getInstance();
            api = new GeneratorAPI(plugin.getGeneratorManager(), plugin.getRefundManager(), plugin.getUserManager(),
                    plugin.getWorthManager(), plugin.getSellwandManager(), plugin.getEventManager());
        }
        return api;
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public RefundManager getRefundManager() {
        return refundManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public WorthManager getWorthManager() {
        return worthManager;
    }

    public SellwandManager getSellwandManager() {
        return sellwandManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public SellManager getSellManager() {
        return sellManager;
    }

    public SellMultiplierRegistry getMultiplierRegistry() {
        return sellMultiplierRegistry;
    }

    public DatabaseManager getDatabaseManager() {
        return dbm;
    }

    public BoltAPI getBoltAPI() {
        return boltAPI;
    }

    public static NextGens getInstance() {
        return instance;
    }

}
