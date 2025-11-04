package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.gui.UpgradeGensInventory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class UpgradeGensCommand extends RoutedCommand {

    public static void registerCommand() {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("commands.upgrade_gens.enabled")) {
            String command = config.getString("commands.upgrade_gens.command");
            new UpgradeGensCommand(command);
        }
    }

    public UpgradeGensCommand(String command) {
        super(command, "nextgens.upgradegens");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.upgrade_gens.aliases"));

        root()
                .exec((sender, ctx) -> {
                    if (!(sender instanceof Player player))
                        return;
                    // Open up the upgrade gens gui
                    new UpgradeGensInventory(player).open(player);
                });

        // Register the command
        register();
    }

}
