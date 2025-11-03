package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.events.Event;
import com.muhammaddaffa.nextgens.events.managers.EventManager;

import java.util.List;

public class StartEventCommand {

    public static void handle(RoutedCommand.CommandPlan plan, EventManager manager) {
        plan.perm("nextgens.admin")
                .arg("event", new StringArg(), ArgSuggester.ofDynamic((sender, prefix) -> {
                    List<String> suggestions = manager.getEventName();
                    suggestions.add("random");

                    return suggestions;
                }))
                .exec((sender, ctx) -> {
                    String eventId = ctx.get("event", String.class);
                    // if there is an event running
                    if (manager.getActiveEvent() != null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.event-is-running");
                        return;
                    }
                    Event event;
                    if (eventId.equalsIgnoreCase("random")) {
                        event = manager.getRandomEvent();
                    } else {
                        event = manager.getEvent(eventId);
                    }
                    // check if event is invalid
                    if (event == null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.invalid-event");
                        return;
                    }
                    // actually start the event
                    manager.forceStart(event);
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.event-start", new Placeholder()
                            .add("{event}", event.getDisplayName()));
                });
    }

}
