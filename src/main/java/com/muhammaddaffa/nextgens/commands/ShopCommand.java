package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.gui.ShopInventory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ShopCommand extends RoutedCommand {

    public static void registerCommand(GeneratorManager manager) {
        // check if the command is enabled
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("commands.shop.enabled")) {
            String command = config.getString("commands.shop.command");
            new ShopCommand(command, manager);
        }
    }

    public ShopCommand(String command, GeneratorManager manager) {
        super(command, "nextgens.shop");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.shop.aliases"));

        root()
                .argOptional("target", new OnlinePlayerArg())
                .exec((sender, ctx) -> {
                    Player target = ctx.get("target", Player.class);
                    Player actualTarget = null;

                    if (target == null) {
                        if (!(sender instanceof Player player)) {
                            Common.sendMessage(sender, "&cUsage: /{command} <player>", new Placeholder()
                                    .add("{command}", command));
                            return;
                        }
                        actualTarget = player;
                    } else {
                        if (!sender.hasPermission("nextgens.shop.others")) {
                            NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                            return;
                        }
                        actualTarget = target;
                    }
                    // open the gui for the target
                    ShopInventory.openInventory(actualTarget, manager);
                });


        // Register this command
        register();
    }

}
