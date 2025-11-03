package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class GiveCommand {

    public static void handle(RoutedCommand.CommandPlan plan, GeneratorManager manager) {
        plan.perm("nextgens.admin")
                .arg("target", new OnlinePlayerArg())
                .arg("id", new StringArg(), ArgSuggester.ofList(new ArrayList<>(manager.getGeneratorIDs())))
                .argOptional("amount", new IntArg())
                .exec((sender, ctx) -> {
                    Player target = ctx.get("target", Player.class);
                    String generatorId = ctx.get("id", String.class);
                    Integer amount = ctx.get("amount", Integer.class);

                    Generator generator = manager.getGenerator(generatorId);
                    if (generator == null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.invalid-gen");
                        return;
                    }
                    int actualAmount = 1;
                    if (amount != null) {
                        actualAmount = Math.max(1, amount);
                    }
                    // actually give the item to the player
                    Common.addInventoryItem(target, generator.createItem(actualAmount));
                    // send message to the sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.give-gen", new Placeholder()
                            .add("{amount}", actualAmount)
                            .add("{gen}", generator.displayName())
                            .add("{player}", target.getName()));
                    // send message to the receiver
                    NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.receive-gen", new Placeholder()
                            .add("{amount}", actualAmount)
                            .add("{gen}", generator.displayName()));

                });
    }

}
