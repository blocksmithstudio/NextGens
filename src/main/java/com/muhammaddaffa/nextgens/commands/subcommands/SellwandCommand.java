package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.builtin.DoubleArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
import org.bukkit.entity.Player;

public class SellwandCommand {

    public static void handle(RoutedCommand.CommandPlan plan, SellwandManager manager) {
        plan.perm("nextgens.admin")
                .alias("sellwands")
                .arg("target", new OnlinePlayerArg())
                .arg("multiplier", new DoubleArg())
                .arg("uses", new IntArg())
                .exec((sender, ctx) -> {
                    Player target = ctx.get("target", Player.class);
                    double multiplier = ctx.get("multiplier", Double.class);
                    int uses = ctx.get("uses", Integer.class);

                    Common.addInventoryItem(target, manager.create(multiplier, uses));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.sellwand-give", new Placeholder()
                            .add("{player}", target.getName())
                            .add("{multiplier}", Common.digits(multiplier))
                            .add("{uses}", manager.getUsesPlaceholder(uses)));
                    NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.sellwand-receive", new Placeholder()
                            .add("{multiplier}", Common.digits(multiplier))
                            .add("{uses}", manager.getUsesPlaceholder(uses)));
                });
    }

}
