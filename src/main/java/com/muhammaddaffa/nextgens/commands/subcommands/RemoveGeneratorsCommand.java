package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.entity.Player;

public class RemoveGeneratorsCommand {

    public static void handle(RoutedCommand.CommandPlan plan, GeneratorManager manager) {
        plan.perm("nextgens.admin")
                .arg("player", new OnlinePlayerArg())
                .exec((sender, ctx) -> {
                    Player player = ctx.get("player", Player.class);
                    manager.removeAllGenerator(player);
                    // Send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.remove-all", new Placeholder()
                            .add("{player}", player.getName()));
                });
    }

}
