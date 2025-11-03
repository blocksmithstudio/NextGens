package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.GeneratorAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WorthCommand extends RoutedCommand {

    public static void registerCommand() {
        // check if the command is enabled
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("commands.worth.enabled")) {
            // Register this command
            String command = config.getString("commands.worth.command");
            new WorthCommand(command);
        }
    }

    public WorthCommand(String command) {
        super(command, "nextgens.worth");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.worth.aliases"));

        // Execute the command
        root()
                .exec((sender, ctx) -> {
                    if (!(sender instanceof Player player))
                        return;
                    // get variables we need
                    GeneratorAPI api = NextGens.getApi();
                    ItemStack stack = player.getInventory().getItemInMainHand();
                    // get the worth of the item
                    Double worth = api.getWorth(stack);
                    // if worth is null, let player know
                    if (worth == null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.item-worthless");
                        return;
                    }
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.item-worth", new Placeholder()
                            .add("{worth}", Common.digits(worth)));
                });

        // Register this command
        register();
    }

}
