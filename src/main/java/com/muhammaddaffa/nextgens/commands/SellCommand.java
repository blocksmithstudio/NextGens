package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.*;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.users.UserManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SellCommand extends RoutedCommand {

    public static void registerCommand() {
        // check if sell command is enabled
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("sell-command")) {
            String command = config.getString("commands.sell.command");
            // Register command a tick later
            Executor.sync(() -> {
                Logger.info("Sell command is enabled, overriding and registering sell command...");
                // unregister the command
                try {
                    MDLib.getCommandRegistry().unregister(command);
                } catch (Exception ignored) { }
                // register back the command
                new SellCommand(command);
            });
        }
    }

    public SellCommand(String command) {
        super(command, "nextgens.sell");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.sell.aliases"));

        root()
                .argOptional("target", new OnlinePlayerArg())
                .exec((sender, ctx) -> {
                   Player target = ctx.get("target", Player.class);
                   Player finalTarget = null;

                    if (target == null) {
                        if (!(sender instanceof Player player)) {
                            Common.sendMessage(sender, "&cUsage: /{command} <player>", new Placeholder()
                                    .add("{command}", command));
                            return;
                        }
                        finalTarget = player;
                    } else {
                        if (!sender.hasPermission("nextgens.sell.others")) {
                            NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                            return;
                        }
                        finalTarget = target;
                    }
                    // perform the sell
                    NextGens.getInstance()
                            .getSellManager()
                            .performSell(finalTarget, null, finalTarget.getInventory());
                });

        // Register this command
        register();
    }

}
