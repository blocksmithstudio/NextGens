package com.muhammaddaffa.nextgens.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.*;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorFixHelper;
import com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorParticle;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.utils.*;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

public class FixInventory extends FastInv {

    private final Player player;
    private final ActiveGenerator active;
    private final Generator generator;
    private final UserManager userManager;
    private final GeneratorManager generatorManager;

    public FixInventory(Player player, ActiveGenerator active, Generator generator, UserManager userManager, GeneratorManager generatorManager) {
        super(Settings.CORRUPT_GUI_SIZE, Common.color(Settings.CORRUPT_GUI_TITLE));
        this.player = player;
        this.active = active;
        this.generator = generator;
        this.userManager = userManager;
        this.generatorManager = generatorManager;

        this.setAcceptButton();
        this.setCancelButton();
        this.setDisplayButton();
    }

    private void setDisplayButton() {
        // get the slots
        List<Integer> slots = Settings.CORRUPT_GUI_DISPLAY_SLOTS;
        // if player has enough money
        if (VaultEconomy.getBalance(this.player) >= this.generator.fixCost()) {
            this.setFixButton(slots);
        } else {
            this.setNoMoneyButton(slots);
        }
    }

    private void setFixButton(List<Integer> slots) {
        FileConfiguration config = NextGens.CORRUPT_GUI_CONFIG.getConfig();
        // build the item
        ItemBuilder builder = new ItemBuilder(this.generator.item().getType())
                .name(config.getString("display-enough-money.display-name"))
                .customModelData(config.getInt("display-enough-money.custom-model-data"))
                .lore(config.getStringList("display-enough-money.lore"))
                .flags(ItemFlag.values())
                .placeholder(new Placeholder()
                        .add("{current}", this.generator.displayName())
                        .add("{speed}", this.generator.interval())
                        .add("{cost}", Common.digits(this.generator.fixCost()))
                        .add("{balance}", Common.digits(VaultEconomy.getBalance(this.player))));

        if (config.getBoolean("display-enough-money.glowing")) {
            builder.enchant(Enchantment.INFINITY);
        }

        // set the item
        this.setItems(slots, builder.build(), event -> {
            GeneratorFixHelper.fixGenerator(player, active, generator);
            // close the inventory
            this.player.closeInventory();
        });
    }

    private void setNoMoneyButton(List<Integer> slots) {
        FileConfiguration config = NextGens.CORRUPT_GUI_CONFIG.getConfig();
        // build the item
        ItemBuilder builder = new ItemBuilder(this.generator.item().getType())
                .name(config.getString("display-no-money.display-name"))
                .customModelData(config.getInt("display-no-money.custom-model-data"))
                .lore(config.getStringList("display-no-money.lore"))
                .flags(ItemFlag.values())
                .placeholder(new Placeholder()
                        .add("{current}", this.generator.displayName())
                        .add("{speed}", this.generator.interval())
                        .add("{cost}", Common.digits(this.generator.fixCost()))
                        .add("{balance}", Common.digits(VaultEconomy.getBalance(this.player))));

        if (config.getBoolean("display-no-money.glowing")) {
            builder.enchant(Enchantment.INFINITY);
        }

        // set the item
        this.setItems(Utils.convertListToIntArray(slots), builder.build(), event -> {
            NextGens.DEFAULT_CONFIG.sendMessage(this.player, "messages.not-enough-money", new Placeholder()
                    .add("{money}", Common.digits(VaultEconomy.getBalance(this.player)))
                    .add("{upgradecost}", Common.digits(this.generator.fixCost()))
                    .add("{remaining}", Common.digits(VaultEconomy.getBalance(this.player) - this.generator.fixCost())));
            // play bass sound
            Utils.bassSound(player);
            // close the gui
            this.player.closeInventory();
        });
    }

    private void setAcceptButton() {
        FileConfiguration config = NextGens.CORRUPT_GUI_CONFIG.getConfig();
        // get the slots
        List<Integer> slots = config.getIntegerList("confirm-slots");
        // create the item
        ItemBuilder builder = ItemBuilder.fromConfig(config, "confirm-button");
        if (builder == null) {
            return;
        }
        // set the item
        this.setItems(Utils.convertListToIntArray(slots), builder.build(), event -> {
            Block block = this.active.getLocation().getBlock();
            // money check
            if (VaultEconomy.getBalance(this.player) < this.generator.fixCost()) {
                NextGens.DEFAULT_CONFIG.sendMessage(this.player, "messages.not-enough-money", new Placeholder()
                        .add("{money}", Common.digits(VaultEconomy.getBalance(this.player)))
                        .add("{upgradecost}", Common.digits(this.generator.fixCost()))
                        .add("{remaining}", Common.digits(VaultEconomy.getBalance(this.player) - this.generator.fixCost())));
                // play bass sound
                Utils.bassSound(this.player);
                // close the gui
                this.player.closeInventory();
                return;
            }
            // take the money from player
            VaultEconomy.withdraw(this.player, this.generator.fixCost());
            // fix the generator
            this.active.setCorrupted(false);
            // visual actions
            VisualAction.send(this.player, NextGens.DEFAULT_CONFIG.getConfig(), "corrupt-fix-options", new Placeholder()
                    .add("{gen}", this.generator.displayName())
                    .add("{cost}", Common.digits(this.generator.fixCost())));
            // play particle
            ExecutorManager.getProvider().async(() -> {
                if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("corrupt-fix-options.particles")) {
                    GeneratorParticle.successParticle(block, generator);
                }
            });
            // close the inventory
            this.player.closeInventory();
        });
    }

    private void setCancelButton() {
        FileConfiguration config = NextGens.CORRUPT_GUI_CONFIG.getConfig();
        // get the slots
        List<Integer> slots = config.getIntegerList("cancel-slots");
        // create the item
        ItemBuilder builder = ItemBuilder.fromConfig(config, "cancel-button");
        if (builder == null) {
            return;
        }
        // set the item
        this.setItems(Utils.convertListToIntArray(slots), builder.build(), event -> {
            // close the inventory
            this.player.closeInventory();
        });
    }

}
