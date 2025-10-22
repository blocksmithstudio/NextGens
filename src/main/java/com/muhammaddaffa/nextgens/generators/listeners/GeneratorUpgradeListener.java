package com.muhammaddaffa.nextgens.generators.listeners;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.*;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.events.generators.GeneratorUpgradeEvent;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.action.InteractAction;
import com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorFixHelper;
import com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorUpdateHelper;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.gui.FixInventory;
import com.muhammaddaffa.nextgens.gui.UpgradeInventory;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public record GeneratorUpgradeListener(
        GeneratorManager generatorManager,
        UserManager userManager
) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private void generatorUpgrade(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (event.getHand() != EquipmentSlot.HAND ||
                block == null ||
                NextGens.STOPPING) {
            return;
        }
        // get variables
        ActiveGenerator active = this.generatorManager.getActiveGenerator(block);
        InteractAction action = InteractAction.find(event, InteractAction.SHIFT_RIGHT);
        // skip if not active generator
        if (active == null || action == null) {
            return;
        }
        // get the generator
        Generator generator = active.getGenerator();
        // corruption check
        if (active.isCorrupted()) {
            // get the correct interaction type
            InteractAction required = InteractAction.find(config.getString("interaction.gens-fix"), InteractAction.SHIFT_RIGHT);
            if (action == required) {
                if (config.getBoolean("repair-owner-only") && !this.hasAccess(active, player)) {
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.not-owner");
                    // play bass sound
                    Utils.bassSound(player);
                    return;
                }
                if (config.getBoolean("corruption.gui-fix")) {
                    // create gui
                    FixInventory gui = new FixInventory(player, active, generator, this.userManager, this.generatorManager);
                    // open the gui
                    gui.open(player);
                } else {
                    GeneratorFixHelper.fixGenerator(player, active, generator);
                }
            }
            return;
        }
        // check if player is the owner
        if (!this.hasAccess(active, player)) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.not-owner");
            // play bass sound
            Utils.bassSound(player);
            return;
        }
        // get correct interaction type
        InteractAction required = InteractAction.find(config.getString("interaction.gens-upgrade"), InteractAction.SHIFT_RIGHT);
        if (action != required) {
            return;
        }
        // check for next tier
        Generator nextGenerator = this.generatorManager.getGenerator(generator.nextTier());
        // upgrade gui option
        if (config.getBoolean("upgrade-gui")) {
            // create the gui object
            new UpgradeInventory(player, active, generator, nextGenerator, this.generatorManager, this.userManager)
                    .open(player);
        } else {
            GeneratorUpdateHelper.upgradeGenerator(player, active, generator, nextGenerator);
        }
    }

    private boolean hasAccess(ActiveGenerator active, Player player) {
        // Get the User object of the owner
        User user = this.userManager.getUser(active.getOwner());
        if (user.isMember(player.getUniqueId())) {
            return true;
        }
        return active.getOwner().equals(player.getUniqueId());
    }
}
