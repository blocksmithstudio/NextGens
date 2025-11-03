package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.commands.subcommands.*;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.worth.WorthManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MainCommand extends RoutedCommand {

    public static void registerCommand(GeneratorManager generatorManager, UserManager userManager, EventManager eventManager,
                                WorthManager worthManager, SellwandManager sellwandManager) {

        String command = NextGens.DEFAULT_CONFIG.getString("commands.nextgens.command");
        new MainCommand(
                command,
                generatorManager,
                userManager,
                eventManager,
                worthManager,
                sellwandManager
        );
    }


    public MainCommand(String command,
                       GeneratorManager generatorManager,
                       UserManager userManager,
                       EventManager eventManager,
                       WorthManager worthManager,
                       SellwandManager sellwandManager) {
        super(command, null);

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.nextgens.aliases"));

        // Set the root command
        root()
                .exec((sender, ctx) -> {
                    if (sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.help");
                    }
                });

        // Register the sub commands
        AddMultiplierCommand.handle(sub("addmultiplier"), userManager);
        RemoveMultiplierCommand.handle(sub("removemultiplier"), userManager);
        SetMultiplierCommand.handle(sub("setmultiplier"), userManager);

        AddMaxCommand.handle(sub("addmax"), userManager, generatorManager);
        RemoveMaxCommand.handle(sub("removemax"), userManager, generatorManager);
        ResetMaxCommand.handle(sub("resetmax"), userManager, generatorManager);

        GiveCommand.handle(sub("give"), generatorManager);
        RepairCommand.handle(sub("repair"), generatorManager);
        ReloadCommand.handle(sub("reload"), generatorManager, eventManager, worthManager);
        SellwandCommand.handle(sub("sellwand"), sellwandManager);

        StartEventCommand.handle(sub("startevent"), eventManager);
        StopEventCommand.handle(sub("stopevent"), eventManager);

        StartCorruptionCommand.handle(sub("startcorruption"));
        ViewCommand.handle(sub("view"), generatorManager, userManager);
        RemoveGeneratorsCommand.handle(sub("removegenerators"), generatorManager);

        TrustCommand.handle(sub("trust"), userManager);

        // Register this command
        register();
    }

}
