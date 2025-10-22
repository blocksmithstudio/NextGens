package com.muhammaddaffa.nextgens.commands;

import com.muhammaddaffa.mdlib.commandapi.CommandAPICommand;
import com.muhammaddaffa.mdlib.commandapi.arguments.*;
import com.muhammaddaffa.mdlib.fastinv.FastInvManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.events.Event;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.generators.runnables.CorruptionTask;
import com.muhammaddaffa.nextgens.generators.runnables.GeneratorTask;
import com.muhammaddaffa.nextgens.gui.ViewInventory;
import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
import com.muhammaddaffa.nextgens.users.UserRepository;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.utils.Settings;
import com.muhammaddaffa.nextgens.worth.WorthManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MainCommand {

    public static void register(GeneratorManager generatorManager, UserManager userManager, EventManager eventManager,
                                WorthManager worthManager, SellwandManager sellwandManager) {
        MainCommand command = new MainCommand(generatorManager, userManager, eventManager, worthManager, sellwandManager);
        // register the command
        command.register();
    }

    private final GeneratorManager generatorManager;
    private final UserManager userManager;
    private final EventManager eventManager;
    private final WorthManager worthManager;
    private final SellwandManager sellwandManager;
    private final CommandAPICommand command;
    public MainCommand(GeneratorManager generatorManager, UserManager userManager, EventManager eventManager,
                       WorthManager worthManager, SellwandManager sellwandManager) {
        this.generatorManager = generatorManager;
        this.userManager = userManager;
        this.eventManager = eventManager;
        this.worthManager = worthManager;
        this.sellwandManager = sellwandManager;
        this.command = new CommandAPICommand(NextGens.DEFAULT_CONFIG.getConfig().getString("commands.nextgens.command"))
                .withSubcommand(this.getGiveSubcommand())
                .withSubcommand(this.getAddMaxSubCommand())
                .withSubcommand(this.getRemoveMaxSubcommand())
                .withSubcommand(this.getResetMaxSubcommand())
                .withSubcommand(this.getRepairSubcommand())
                .withSubcommand(this.getReloadSubcommand())
                .withSubcommand(this.getSellwandSubcommand())
                .withSubcommand(this.getStartEventCommand())
                .withSubcommand(this.getStopEventCommand())
                .withSubcommand(this.getAddMultiplierSubCommand())
                .withSubcommand(this.getRemoveMultiplierSubCommand())
                .withSubcommand(this.getSetMultiplierSubCommand())
                .withSubcommand(this.getStartCorruptionCommand())
                .withSubcommand(this.getViewCommand())
                .withSubcommand(this.getRemoveAllCommand())
                .withSubcommand(this.getAddCommand())
                .withSubcommand(this.getRemoveCommand())
                .withSubcommand(this.getMemberList())
                .executes((sender, args) -> {
                    if (sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.help");
                    }
                });
        List<String> aliases = NextGens.DEFAULT_CONFIG.getConfig().getStringList("commands.nextgens.aliases");
        this.command.setAliases(aliases.toArray(new String[0]));
    }

    public void register() {
        this.command.register();
    }

    private CommandAPICommand getAddMultiplierSubCommand() {
        return new CommandAPICommand("addmultiplier")
                .withAliases("addmulti")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new DoubleArgument("amount"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // get all variables
                    Player player = (Player) args.get("target");
                    double amount = (double) args.get("amount");
                    // get the user object and modify the multiplier
                    User user = this.userManager.getUser(player);
                    user.addMultiplier(amount);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.multiplier-increase", new Placeholder()
                            .add("{player}", player.getName())
                            .add("{multiplier}", Common.digits(amount))
                            .add("{total}", Common.digits(user.getMultiplier())));
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.increased-multiplier", new Placeholder()
                            .add("{multiplier}", Common.digits(amount))
                            .add("{total}", Common.digits(user.getMultiplier())));
                });
    }

    private CommandAPICommand getRemoveMultiplierSubCommand() {
        return new CommandAPICommand("removemultiplier")
                .withAliases("removemulti")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new DoubleArgument("amount"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // get all variables
                    Player player = (Player) args.get("target");
                    double amount = (double) args.get("amount");
                    // get the user object and modify the multiplier
                    User user = this.userManager.getUser(player);
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

    private CommandAPICommand getSetMultiplierSubCommand() {
        return new CommandAPICommand("setmultiplier")
                .withAliases("setmulti")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new DoubleArgument("amount"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // get all variables
                    Player player = (Player) args.get("target");
                    double amount = (double) args.get("amount");
                    // get the user object and modify the multiplier
                    User user = this.userManager.getUser(player);
                    user.setMultiplier(amount);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.set-multiplier", new Placeholder()
                            .add("{player}", player.getName())
                            .add("{multiplier}", Common.digits(amount)));
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.multiplier-set", new Placeholder()
                            .add("{multiplier}", Common.digits(amount)));
                });
    }

    private CommandAPICommand getGiveSubcommand() {
        return new CommandAPICommand("give")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new StringArgument("generator_id")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            return this.generatorManager.getGeneratorIDs()
                                    .toArray(String[]::new);
                        })))
                .withOptionalArguments(new IntegerArgument("generator_amount"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // get all variables
                    Player target = (Player) args.get("target");
                    String generatorId = (String) args.get("generator_id");
                    Integer amount = (Integer) args.get("generator_amount");
                    // get the generator object
                    Generator generator = this.generatorManager.getGenerator(generatorId);
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

    private CommandAPICommand getAddMaxSubCommand() {
        return new CommandAPICommand("addmax")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // get all variables
                    Player target = (Player) args.get("target");
                    int amount = (int) args.get("amount");
                    // actually set the bonus generator place
                    User user = this.userManager.getUser(target);
                    user.addBonus(amount);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message to the command sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.add-max", new Placeholder()
                            .add("{amount}", amount)
                            .add("{player}", target.getName()));
                    // send message to the player
                    NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.max-added", new Placeholder()
                            .add("{amount}", amount)
                            .add("{current}", this.generatorManager.getGeneratorCount(target))
                            .add("{max}", this.userManager.getMaxSlot(target)));
                });
    }

    private CommandAPICommand getRemoveMaxSubcommand() {
        return new CommandAPICommand("removemax")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // get all variables
                    Player target = (Player) args.get("target");
                    int amount = (int) args.get("amount");
                    // actually set the bonus generator place
                    User user = this.userManager.getUser(target);
                    user.removeBonus(amount);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message to the command sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.remove-max", new Placeholder()
                            .add("{amount}", amount)
                            .add("{player}", target.getName()));
                    // send message to the player
                    NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.max-removed", new Placeholder()
                            .add("{amount}", amount)
                            .add("{current}", this.generatorManager.getGeneratorCount(target))
                            .add("{max}", this.userManager.getMaxSlot(target)));
                });
    }

    private CommandAPICommand getResetMaxSubcommand() {
        return new CommandAPICommand("resetmax")
                .withArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    Player target = (Player) args.get("target");
                    // reset the bonus
                    User user = this.userManager.getUser(target);
                    user.setBonus(0);
                    // save the user data afterward
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // send message to the command sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.reset-max", new Placeholder()
                            .add("{player}", target.getName()));
                    // send message to the player
                    NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.max-resetted", new Placeholder()
                            .add("{current}", this.generatorManager.getGeneratorCount(target))
                            .add("{max}", this.userManager.getMaxSlot(target)));
                });
    }

    private CommandAPICommand getRepairSubcommand() {
        return new CommandAPICommand("repair")
                .withArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    Player target = (Player) args.get("target");
                    // repair the generators
                    Executor.async(() -> {
                        this.generatorManager.getActiveGenerator(target).forEach(active -> active.setCorrupted(false));
                        // send message to the command sender
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.player-repair", new Placeholder()
                                .add("{player}", target.getName()));
                        // send message to the player
                        NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.gens-repaired");
                    });
                });
    }

    private CommandAPICommand getReloadSubcommand() {
        return new CommandAPICommand("reload")
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // actually reload the config
                    Config.reload();
                    Settings.init();
                    // remove all holograms
                    GeneratorTask.flush();
                    // load back the generator
                    this.generatorManager.loadGenerators();
                    // refresh the active generator
                    Executor.async(this.generatorManager::refreshActiveGenerator);
                    // events stuff
                    this.eventManager.loadEvents();
                    this.eventManager.refresh();
                    // worth reload
                    this.worthManager.load();
                    // send message to the sender
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.reload");
                    // close all gui
                    FastInvManager.closeAll();
                });
    }

    private CommandAPICommand getSellwandSubcommand() {
        // gens sellwand <player> <multiplier> <uses>
        return new CommandAPICommand("sellwand")
                .withAliases("sellwands")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new DoubleArgument("multiplier"))
                .withArguments(new IntegerArgument("uses"))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    Player target = (Player) args.get("target");
                    double multiplier = (double) args.get("multiplier");
                    int uses = (int) args.get("uses");
                    // give player the sellwand
                    Common.addInventoryItem(target, this.sellwandManager.create(multiplier, uses));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.sellwand-give", new Placeholder()
                            .add("{player}", target.getName())
                            .add("{multiplier}", Common.digits(multiplier))
                            .add("{uses}", this.sellwandManager.getUsesPlaceholder(uses)));
                    NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.sellwand-receive", new Placeholder()
                            .add("{multiplier}", Common.digits(multiplier))
                            .add("{uses}", this.sellwandManager.getUsesPlaceholder(uses)));
                });
    }

    private CommandAPICommand getStartEventCommand() {
        // gens startevent <event>
        return new CommandAPICommand("startevent")
                .withArguments(new StringArgument("event")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            List<String> suggestions = this.eventManager.getEventName();
                            suggestions.add("random");
                            return suggestions.toArray(String[]::new);
                        })))
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // if there is an event running
                    if (this.eventManager.getActiveEvent() != null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.event-is-running");
                        return;
                    }
                    String eventId = (String) args.get("event");
                    Event event;
                    if (eventId.equalsIgnoreCase("random")) {
                        event = this.eventManager.getRandomEvent();
                    } else {
                        event = this.eventManager.getEvent(eventId);
                    }
                    // check if event is invalid
                    if (event == null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.invalid-event");
                        return;
                    }
                    // actually start the event
                    this.eventManager.forceStart(event);
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.event-start", new Placeholder()
                            .add("{event}", event.getDisplayName()));
                });
    }

    private CommandAPICommand getStopEventCommand() {
        // gens stopevent
        return new CommandAPICommand("stopevent")
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    if (this.eventManager.forceEnd()) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.event-stop");
                    } else {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-event");
                    }
                });
    }

    private CommandAPICommand getStartCorruptionCommand() {
        return new CommandAPICommand("startcorruption")
                .executes((sender, args) -> {
                    // permission check
                    if (!sender.hasPermission("nextgens.admin")) {
                        NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.no-permission");
                        return;
                    }
                    // corrupt the generators
                    CorruptionTask.getInstance().corruptGenerators();
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.corrupt-gens");
                });
    }

    private CommandAPICommand getViewCommand() {
        return new CommandAPICommand("view")
                .withPermission("nextgens.view")
                .withOptionalArguments(new StringArgument("name")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            return this.userManager.getUsersName().toArray(String[]::new);
                        })))
                .executesPlayer((player, args) -> {
                    String name = (String) args.getOrDefault("name", player.getName());
                    User user = this.userManager.getUser(name);
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
                    ViewInventory.openInventory(player, user, this.generatorManager, this.userManager);
                });
    }

    private CommandAPICommand getRemoveAllCommand() {
        return new CommandAPICommand("removeall")
                .withPermission("nextgens.admin")
                .withArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    // Remove all generators
                    this.generatorManager.removeAllGenerator(target);
                    // Send message
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.remove-all", new Placeholder()
                            .add("{player}", target.getName()));
                });
    }

    private CommandAPICommand getRemoveCommand() {
        return new CommandAPICommand("remove")
                .withPermission("nextgens.command.remove")
                .withArguments(new OfflinePlayerArgument("target")
                        .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                            Player player = (Player) info.sender();
                            return this.userManager.getUsersMemberName(player).toArray(String[]::new);

                })))
                .executesPlayer((player, args) -> {

                    // Get the target player
                    OfflinePlayer targetPlayer = (OfflinePlayer) args.get("target");

                    // Check if the target has played before
                    if (!targetPlayer.hasPlayedBefore()) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-target");
                        return;
                    }

                    // Get the user object
                    User user = this.userManager.getUser(player.getUniqueId());
                    // Check if the target user is not a member
                    if (!user.isMember(targetPlayer.getUniqueId())) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.not-member", new Placeholder()
                                .add("{player}", user.getName()));
                        return;
                    }
                    // Kick the player
                    user.removeMember(targetPlayer.getUniqueId());
                    // Save the user data
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));
                    // Send messages
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.remove-member", new Placeholder()
                            .add("{player}", targetPlayer.getName()));
                    // Check if the target is online or not
                    Player target = Bukkit.getPlayer(targetPlayer.getUniqueId());
                    if (target != null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(target, "messages.player-removed", new Placeholder()
                                .add("{player}", player.getName()));
                    }
                });
    }

    private CommandAPICommand getAddCommand() {
        return new CommandAPICommand("add")
                .withArguments(new PlayerArgument("target"))
                .executesPlayer((player, args) -> {
                    // get the user object
                    User user = this.userManager.getUser(player);
                    OfflinePlayer targetPlayer = (OfflinePlayer) args.get("target");

                    // check if the target has played before
                    if (!targetPlayer.hasPlayedBefore()) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.invalid-target");
                        return;
                    }

                    // If player and target is the same, cancel it
                    if (player.getUniqueId().equals(targetPlayer.getUniqueId())) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.self-add");
                        return;
                    }

                    // check if the target is already a member
                    if (user.isMember(targetPlayer.getUniqueId())) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.already-member", new Placeholder()
                                .add("{player}", targetPlayer.getName()));
                        return;
                    }

                    // create an invitation
                    user.addMember(targetPlayer.getUniqueId());

                    // save the user
                    Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));

                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.add-member", new Placeholder()
                            .add("{player}", targetPlayer.getName()));
                    // check if the target is online
                    if (targetPlayer.isOnline() && targetPlayer.getPlayer() != null) {
                        NextGens.DEFAULT_CONFIG.sendMessage(targetPlayer.getPlayer(), "messages.player-added", new Placeholder()
                                .add("{player}", player.getName()));
                    }
                });
    }

    private CommandAPICommand getMemberList() {
        return new CommandAPICommand("memberlist")
                .withPermission("nextgens.see.memberlist")
                .executes((sender, args) -> {
                    Player player = (Player) sender;
                    User user = this.userManager.getUser(player);
                    // Send the list of all members
                    NextGens.DEFAULT_CONFIG.sendMessage(sender, "messages.member-list", new Placeholder()
                            .add("{members}",
                                    user.getMemberSet().isEmpty()
                                    ? "&cNo members"
                                    : String.join("\n", user.getMemberNames()))
                            .add("{member_with}",
                                    this.userManager.getWhoUserAddedToPlayer(player).isEmpty()
                                    ? "&cNo one added you"
                                    : String.join("\n", this.userManager.getWhoUserAddedToPlayer(player))));
                });
    }


}
