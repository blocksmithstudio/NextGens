package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.entity.Player;

public class RepairCommand {

    public static void handle(RoutedCommand.CommandPlan plan, GeneratorManager manager) {
        plan.perm("nextgens.admin")
                .arg("player", new OnlinePlayerArg())
                .exec((sender, ctx) -> {
                    Player player = ctx.get("player", Player.class);

                    ExecutorManager.getProvider().async(() -> {
                        manager.getActiveGenerator(player).forEach(active -> active.setCorrupted(false));
                        // send message to the command sender
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.player-repair", new Placeholder()
                                .add("{player}", player.getName()));
                        // send message to the player
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.gens-repaired");
                    });
                });
    }

}
