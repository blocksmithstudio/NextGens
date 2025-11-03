package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.gui.PlayerSettingsInventory;
import com.muhammaddaffa.nextgens.users.UserManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerSettingsCommand extends RoutedCommand {

    public static void registerCommand(UserManager manager) {
        // check if the command is enabled
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("commands.player_settings.enabled")) {
            String command = config.getString("commands.player_settings.command");
            new PlayerSettingsCommand(command, manager);
        }
    }

    public PlayerSettingsCommand(String command, UserManager manager) {
        super(command, "nextgens.settings");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.player_settings.aliases"));

        // Configure the root command
        root()
                .exec((sender, ctx) -> {
                    if (sender instanceof Player player) {
                        PlayerSettingsInventory.openInventory(player, manager);
                    }
                });

        // Register this command
        register();

    }

}
