package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.DoubleArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.entity.Player;

public class SetMultiplierCommand {

    public static void handle(RoutedCommand.CommandPlan plan, UserManager manager) {
        plan.alias("setmulti")
                .perm("nextgens.admin")
                .arg("target", new OnlinePlayerArg())
                .arg("amount", new DoubleArg())
                .exec((sender, ctx) -> {
                    Player player = ctx.get("target", Player.class);
                    double amount = ctx.get("amount", Double.class);

                    User user = manager.getUser(player);
                    user.setMultiplier(amount);
                    // save the user data afterward
                    ExecutorManager.getProvider().async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.set-multiplier", new Placeholder()
                            .add("{player}", player.getName())
                            .add("{multiplier}", Common.digits(amount)));
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.multiplier-set", new Placeholder()
                            .add("{multiplier}", Common.digits(amount)));
                });
    }

}
