package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.fastinv.FastInvManager;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.cache.WorldBoostCache;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.generators.runnables.GeneratorTask;
import com.muhammaddaffa.nextgens.utils.Settings;
import com.muhammaddaffa.nextgens.worth.WorthManager;

public class ReloadCommand {

    public static void handle(RoutedCommand.CommandPlan plan,
                              GeneratorManager generatorManager,
                              EventManager eventManager,
                              WorthManager worthManager) {
        plan.perm("nextgens.admin")
                .exec((sender, ctx) -> {
                    // actually reload the config
                    Config.reload();
                    Settings.init();
                    // remove all holograms
                    GeneratorTask.flush();
                    // load back the generator
                    generatorManager.loadGenerators();
                    // refresh the active generator
                    Executor.async(generatorManager::refreshActiveGenerator);
                    // events stuff
                    eventManager.loadEvents();
                    eventManager.refresh();
                    // worth reload
                    worthManager.load();
                    // cache reload
                    WorldBoostCache.init();
                    // send message to the sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.reload");
                    // close all gui
                    FastInvManager.closeAll();
                });
    }

}
