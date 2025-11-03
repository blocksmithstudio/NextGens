package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.DoubleArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.entity.Player;

public class RemoveMultiplierCommand {

    public static void handle(RoutedCommand.CommandPlan plan, UserManager manager) {
        plan.alias("removemulti")
                .perm("nextgens.admin")
                .arg("target", new OnlinePlayerArg())
                .arg("amount", new DoubleArg())
                .exec((sender, ctx) -> {
                    Player player = ctx.get("target", Player.class);
                    double amount = ctx.get("amount", Double.class);

                    User user = manager.getUser(player);
                    user.removeMultiplier(amount);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.multiplier-decrease", new Placeholder()
                            .add("{player}", player.getName())
                            .add("{multiplier}", Common.digits(amount))
                            .add("{total}", Common.digits(user.getMultiplier())));
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.decreased-multiplier", new Placeholder()
                            .add("{multiplier}", Common.digits(amount))
                            .add("{total}", Common.digits(user.getMultiplier())));
                });
    }

}
