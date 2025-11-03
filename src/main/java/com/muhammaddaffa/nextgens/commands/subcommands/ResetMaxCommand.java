package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.entity.Player;

public class ResetMaxCommand {

    public static void handle(RoutedCommand.CommandPlan plan, UserManager userManager, GeneratorManager generatorManager) {
        plan.perm("nextgens.admin")
                .arg("target", new OnlinePlayerArg())
                .exec((sender, ctx) -> {
                    Player player = ctx.get("target", Player.class);

                    User user = userManager.getUser(player);
                    user.setBonus(0);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message to the command sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.reset-max", new Placeholder()
                            .add("{player}", player.getName()));
                    // send message to the player
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.max-resetted", new Placeholder()
                            .add("{current}", generatorManager.getGeneratorCount(player))
                            .add("{max}", userManager.getMaxSlot(player)));
                });
    }

}
