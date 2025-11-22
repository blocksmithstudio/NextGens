package com.muhammaddaffa.nextgens.commands.subcommands;

import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.builtin.LiteralArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class TrustCommand {

    public static void handle(RoutedCommand.CommandPlan plan, UserManager manager) {
        plan.arg("action", new LiteralArg("add", "remove", "list"))
                .argOptional("playerName", new StringArg(), (sender, prefix, prev) -> {
                    String action = prev.get("action", String.class);
                    if (action == null) {
                        return List.of();
                    }

                    switch (action.toLowerCase()) {
                        case "add" -> {
                            return null;
                        }
                        case "remove" -> {
                            if (!(sender instanceof Player player))
                                return java.util.List.of();
                            return manager.getUsersMemberName(player);
                        }
                        default -> {
                            return java.util.List.of();
                        }
                    }
                })
                .exec((sender, ctx) -> {
                    if (!(sender instanceof Player player))
                        return;

                    String action = ctx.get("action", String.class);
                    String playerName = ctx.get("playerName", String.class);

                    switch (action.toLowerCase()) {
                        case "add" -> add(player, playerName, manager);
                        case "remove" -> remove(player, playerName, manager);
                        case "list" -> list(player, manager);
                    }
                });
    }

    private static void add(Player player, String playerName, UserManager manager) {
        if (playerName == null || playerName.isEmpty()) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-user");
            return;
        }

        User user = manager.getUser(player);
        // Check if player tries to add themselves
        if (player.getName().equalsIgnoreCase(playerName)) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.self-add");
            return;
        }
        // check if the target has played before
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-target");
            return;
        }
        // Check if target is already a member
        if (user.isMember(targetPlayer.getUniqueId())) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.already-member", new Placeholder()
                    .add("{player}", playerName));
            return;
        }
        user.addMember(targetPlayer.getUniqueId());
        // save the user
        ExecutorManager.getProvider().async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
        // send message
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.add-member", new Placeholder()
                .add("{player}", targetPlayer.getName()));
        // check if the target is online
        if (targetPlayer.isOnline() && targetPlayer.getPlayer() != null) {
            NextGens.DEFAULT_CONFIG.sendMessage(targetPlayer.getPlayer(), "messages.player-added", new Placeholder()
                    .add("{player}", player.getName()));
        }
    }

    private static void remove(Player player, String playerName, UserManager manager) {
        if (playerName == null || playerName.isEmpty()) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-user");
            return;
        }
        // Get the user object from player
        User user = manager.getUser(player);
        if (!user.isMember(playerName)) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.not-member", new Placeholder()
                    .add("{player}", playerName));
            return;
        }
        user.removeMember(playerName);
        // Save the user data
        ExecutorManager.getProvider().async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
        // Send messages
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.remove-member", new Placeholder()
                .add("{player}", playerName));
        // Check if the target is online or not
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.player-removed", new Placeholder()
                    .add("{player}", player.getName()));
        }
    }

    private static void list(Player player, UserManager manager) {
        User user = manager.getUser(player);
        // Send the list of all members
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.member-list", new Placeholder()
                .add("{members}",
                        user.getMemberSet().isEmpty()
                                ? "&cNo members"
                                : String.join(", ", user.getMemberNames()))
                .add("{member_with}",
                        manager.getWhoUserAddedToPlayer(player).isEmpty()
                                ? "&cNo one added you"
                                : String.join(", ", manager.getWhoUserAddedToPlayer(player))));
    }

}
