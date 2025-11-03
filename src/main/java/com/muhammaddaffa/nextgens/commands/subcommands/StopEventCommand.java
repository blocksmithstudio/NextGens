package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.events.managers.EventManager;

public class StopEventCommand {

    public static void handle(RoutedCommand.CommandPlan plan, EventManager manager) {
        plan.perm("nextgens.admin")
                .exec((sender, ctx) -> {
                    if (manager.forceEnd()) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.event-stop");
                    } else {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-event");
                    }
                });
    }

}
