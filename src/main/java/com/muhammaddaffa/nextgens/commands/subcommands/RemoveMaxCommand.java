package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.entity.Player;

public class RemoveMaxCommand {

    public static void handle(RoutedCommand.CommandPlan plan, UserManager userManager, GeneratorManager generatorManager) {
        plan.perm("nextgens.admin")
                .arg("target", new OnlinePlayerArg())
                .arg("amount", new IntArg())
                .exec((sender, ctx) -> {
                    Player player = ctx.get("target", Player.class);
                    int amount = ctx.get("amount", Integer.class);

                    User user = userManager.getUser(player);
                    user.removeBonus(amount);
                    // save the user data afterward
                    ExecutorManager.getProvider().async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message to the command sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.remove-max", new Placeholder()
                            .add("{amount}", amount)
                            .add("{player}", player.getName()));
                    // send message to the player
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.max-removed", new Placeholder()
                            .add("{amount}", amount)
                            .add("{current}", generatorManager.getGeneratorCount(player))
                            .add("{max}", userManager.getMaxSlot(player)));

                });
    }

}
