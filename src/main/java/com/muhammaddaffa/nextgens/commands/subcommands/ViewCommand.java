package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.gui.ViewInventory;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.entity.Player;

public class ViewCommand {

    public static void handle(RoutedCommand.CommandPlan plan,
                              GeneratorManager generatorManager,
                              UserManager userManager) {
        plan.perm("nextgens.view")
                .argOptional("name", new StringArg(), ArgSuggester.ofList(userManager.getUsersName()))
                .exec((sender, ctx) -> {
                    if (!(sender instanceof Player player))
                        return;

                    String name = ctx.get("name", String.class);
                    User user = userManager.getUser(name);
                    if (user == null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-user");
                        return;
                    }
                    // if player is not the user
                    if (!user.getUniqueId().equals(player.getUniqueId()) &&
                            !player.hasPermission("nextgens.view.others")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.no-permission");
                        return;
                    }
                    // open the inventory
                    ViewInventory.openInventory(player, user, generatorManager, userManager);
                });
    }

}
