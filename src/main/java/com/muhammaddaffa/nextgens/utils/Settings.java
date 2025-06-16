package com.muhammaddaffa.nextgens.utils;

import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.nextgens.NextGens;
import jdk.dynalink.linker.LinkerServices;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class Settings {

    public static boolean FORCE_UPDATE_BLOCKS, AUTO_SAVE_ENABLED, CORRUPTION_ENABLED, CORRUPTION_GUI_FIX,
            CORRUPTION_HOLOGRAM, DEFAULT_MAX_GENERATOR_ENABLED, SELL_COMMAND, PLACE_PERMISSION, ONLINE_ONLY,
            ANTI_EXPLOSION, DISABLE_DROP_PLACE, ISLAND_PICKUP,
            UPGRADE_GUI, CLOSE_ON_PURCHASE, CLOSE_ON_NO_MONEY, DROP_ON_BREAK, BROKEN_PICKUP, REPAIR_OWNER_ONLY,
            DISABLE_CRAFTING, GENERATOR_PLACE_DISTANCE, CORRUPTION_ONLINE_ONLY;

    // String
    public static String GENS_PICKUP_ACTION, GENS_UPGRADE_ACTION, GENS_FIX_ACTION, CORRUPT_GUI_TITLE;

    // String List
    public static List<String> BLACKLISTED_WORLDS, CORRUPTION_BLACKLISTED_GENERATORS, CORRUPTION_HOLOGRAM_LINES;

    // Integer
    public static int CORRUPTION_PERCENTAGE, CORRUPTION_INTERVAL, CORRUPTION_NOTIFY_INTERVAL, CORRUPT_GUI_SIZE;

    // Integer List
    public static List<Integer> CORRUPT_GUI_DISPLAY_SLOTS;

    // Double
    public static double CORRUPTION_HOLOGRAM_HEIGHT;

    // Config Message
    public static ConfigMessage CORRUPTION_BROADCAST, CORRUPTION_NOTIFY_MESSAGE;

    public static void init() {
        config(); corruptGui();
    }

    public static void config() {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();

        // Boolean
        FORCE_UPDATE_BLOCKS = config.getBoolean("force-update-blocks");
        AUTO_SAVE_ENABLED = config.getBoolean("auto-save.enabled");
        CORRUPTION_ENABLED = config.getBoolean("corruption.enabled");
        CORRUPTION_GUI_FIX = config.getBoolean("corruption.gui-fix");
        CORRUPTION_HOLOGRAM = config.getBoolean("corruption.hologram.enabled");
        DEFAULT_MAX_GENERATOR_ENABLED = config.getBoolean("default-max-generator.enabled");
        SELL_COMMAND = config.getBoolean("sell-command");
        PLACE_PERMISSION = config.getBoolean("place-permission");
        ONLINE_ONLY = config.getBoolean("online-only");
        ANTI_EXPLOSION = config.getBoolean("anti-explosion");
        DISABLE_DROP_PLACE = config.getBoolean("disable-drop-place");
        ISLAND_PICKUP = config.getBoolean("island-pickup");
        UPGRADE_GUI = config.getBoolean("upgrade-gui");
        CLOSE_ON_PURCHASE = config.getBoolean("close-on-purchase");
        CLOSE_ON_NO_MONEY = config.getBoolean("close-on-no-money");
        DROP_ON_BREAK = config.getBoolean("drop-on-break");
        BROKEN_PICKUP = config.getBoolean("broken-pickup");
        REPAIR_OWNER_ONLY = config.getBoolean("repair-owner-only");
        DISABLE_CRAFTING = config.getBoolean("disable-crafting.enabled");
        GENERATOR_PLACE_DISTANCE = config.getBoolean("generator-place-distance.enabled");
        CORRUPTION_ONLINE_ONLY = config.getBoolean("corruption.online-only");

        // String
        GENS_PICKUP_ACTION = config.getString("interaction.gens-pickup");
        GENS_UPGRADE_ACTION = config.getString("interaction.gens-upgrade");
        GENS_FIX_ACTION = config.getString("interaction.gens-fix");

        // String List
        BLACKLISTED_WORLDS = config.getStringList("blacklisted-worlds");
        CORRUPTION_BLACKLISTED_GENERATORS = config.getStringList("corruption.blacklisted-generators");
        CORRUPTION_HOLOGRAM_LINES = config.getStringList("corruption.hologram.lines");

        // Integer
        CORRUPTION_PERCENTAGE = config.getInt("corruption.percentage");
        CORRUPTION_INTERVAL = config.getInt("corruption.interval");
        CORRUPTION_NOTIFY_INTERVAL = config.getInt("corruption.notify.interval");

        // Double
        CORRUPTION_HOLOGRAM_HEIGHT = config.getDouble("corruption.hologram.height");

        // Config Message
        CORRUPTION_BROADCAST = new ConfigMessage(config, "corruption.broadcast");
        CORRUPTION_NOTIFY_MESSAGE = new ConfigMessage(config, "corruption.notify.messages");
    }

    public static void corruptGui() {
        FileConfiguration config = NextGens.CORRUPT_GUI_CONFIG.getConfig();

        // String
        CORRUPT_GUI_TITLE = config.getString("title");

        // Integer
        CORRUPT_GUI_SIZE = config.getInt("size");

        // Integer List
        CORRUPT_GUI_DISPLAY_SLOTS = config.getIntegerList("display-slots");
    }

}
