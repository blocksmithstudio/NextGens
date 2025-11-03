package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorFixHelper;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RepairGensCommand extends RoutedCommand {

    public static void registerCommand(GeneratorManager manager) {
        // check if the command is enabled
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("commands.repair_gens.enabled")) {
            String command = config.getString("commands.repair_gens.command");
            new RepairGensCommand(command, manager);
        }
    }

    public RepairGensCommand(String command, GeneratorManager manager) {
        super(command, "nextgens.repairgens");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.repair_gens.aliases"));

        // Configure the root command
        root()
                .exec((sender, ctx) -> {
                    if (sender instanceof Player player) {
                        GeneratorFixHelper.fixGenerators(player, manager);
                    }
                });

        // Register this command
        register();
    }

}
