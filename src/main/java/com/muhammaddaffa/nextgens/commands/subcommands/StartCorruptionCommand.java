package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.runnables.CorruptionTask;

public class StartCorruptionCommand {

    public static void handle(RoutedCommand.CommandPlan plan) {
        plan.perm("nextgens.admin")
                .exec((sender, ctx) -> {
                    CorruptionTask.getInstance().corruptGenerators();
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.corrupt-gens");
                });
    }

}
