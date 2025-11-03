package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.xseries.XSound;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class PickupCommand extends RoutedCommand {

    public static void registerCommand(GeneratorManager manager) {
        // check if the command is enabled
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("commands.pickup.enabled")) {
            String command = config.getString("commands.pickup.command");
            new PickupCommand(command, manager);
        }
    }

    public PickupCommand(String command, GeneratorManager manager) {
        super(command, "nextgens.pickup");

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // Set the aliases
        alias(config.getStringList("commands.pickup.aliases"));

        root()
                .argOptional("target", new OnlinePlayerArg())
                .exec((sender, ctx) -> {
                    Player target = ctx.get("target", Player.class);
                    Player actualTarget;

                    // Early stop to prevent dupe
                    if (NextGens.STOPPING)
                        return;

                    if (target == null) {
                        if (!(sender instanceof Player player)) {
                            Common.sendMessage(sender, "&cUsage: /{command} <player>", new Placeholder()
                                    .add("{command}", command));
                            return;
                        }
                        actualTarget = player;
                    } else {
                        if (!sender.hasPermission("nextgens.pickup.others")) {
                            NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                            return;
                        }
                        actualTarget = target;
                    }
                    // Pickup generators for the actual target
                    this.pickupGenerators(sender, actualTarget, manager);
                });

        // Register this command
        register();
    }

    private void pickupGenerators(CommandSender sender, Player player, GeneratorManager manager) {
        // get all generators player have
        List<ActiveGenerator> generators = manager.getActiveGenerator(player);
        int total = generators.size();
        // loop generators
        for (ActiveGenerator active : generators) {
            // check if broken pickup option is enabled
            if (!NextGens.DEFAULT_CONFIG.getConfig().getBoolean("broken-pickup") && active.isCorrupted()) {
                total--;
                continue;
            }
            // unregister the generator
            manager.unregisterGenerator(active.getLocation());
            // set the block to air
            active.getLocation().getBlock().setType(Material.AIR);
            // give the item to the player
            Common.addInventoryItem(player, active.getGenerator().createItem(1));
        }
        // send message
        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.force-pickup", new Placeholder()
                .add("{player}", player.getName())
                .add("{amount}", Common.digits(total)));
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.pickup-gens", new Placeholder()
                .add("{amount}", Common.digits(total)));
        // send sound
        player.playSound(player.getLocation(), XSound.ENTITY_PLAYER_LEVELUP.get(), 1.0f, 2.0f);
    }

}
