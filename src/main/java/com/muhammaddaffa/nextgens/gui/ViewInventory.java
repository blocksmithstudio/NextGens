package com.muhammaddaffa.nextgens.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.xseries.XSound;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.gui.helpers.Pagination;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import com.muhammaddaffa.nextgens.utils.VisualAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ViewInventory extends FastInv {

    public static void openInventory(Player player, User user, GeneratorManager generatorManager, UserManager userManager) {
        // create the gui
        ViewInventory gui = new ViewInventory(player, user, generatorManager, userManager);
        // open the gui
        gui.open(player);
    }

    private final Player player;
    private final User user;
    private final GeneratorManager generatorManager;
    private final UserManager userManager;

    private Pagination<ActiveGenerator> pagination;

    public ViewInventory(Player player, User user, GeneratorManager generatorManager, UserManager userManager) {
        super(NextGens.VIEW_GUI_CONFIG.getInt("size"), Common.color(NextGens.VIEW_GUI_CONFIG.getString("title")
                .replace("{player}", user.getName())));
        this.player = player;
        this.user = user;
        this.generatorManager = generatorManager;
        this.userManager = userManager;

        this.setAllItems();
    }

    private void setAllItems() {
        FileConfiguration config = NextGens.VIEW_GUI_CONFIG.getConfig();
        List<ActiveGenerator> generators = this.generatorManager.getActiveGenerator(user.getUniqueId());
        List<Integer> slots = config.getIntegerList("slots");
        if (this.pagination == null) {
            this.pagination = new Pagination<>(generators, slots);
        }

        // clear the inventory first
        this.clearItems();

        List<String> additionalLore = config.getStringList("additional-lore");
        String pickupPermission = config.getString("pickup-own");
        String pickupOthersPermission = config.getString("pickup-others");
        boolean isAdditionalLore = this.player.getUniqueId().equals(user.getUniqueId()) && this.player.hasPermission(pickupPermission);

        if (!this.player.getUniqueId().equals(user.getUniqueId()) && this.player.hasPermission(pickupOthersPermission))
            isAdditionalLore = true;

        List<ActiveGenerator> pageItems = this.pagination.getItems(this.pagination.currentPage);
        for (int i = 0; i < pageItems.size(); i++) {
            ActiveGenerator active = pageItems.get(i);
            Integer slot = slots.get(i);

            ItemBuilder builder = new ItemBuilder(active.getGenerator().createItem(1));
            if (isAdditionalLore) builder.addLore(additionalLore);
            builder.placeholder(new Placeholder()
                    .add("{x}", Common.digits(active.getLocation().getBlockX()))
                    .add("{y}", Common.digits(active.getLocation().getBlockY()))
                    .add("{z}", Common.digits(active.getLocation().getBlockZ()))
                    .add("{world}", active.getLocation().getWorld().getName()));

            boolean finalIsAdditionalLore = isAdditionalLore;
            this.setItem(slot, builder.build(), event -> {
                if (!finalIsAdditionalLore) return;
                // unregister the generator
                this.generatorManager.unregisterGenerator(active.getLocation());
                // set the block to air
                active.getLocation().getBlock().setType(Material.AIR);
                // give the item to the player
                Common.addInventoryItem(this.player, active.getGenerator().createItem(1));
                // add visual action
                Player owner = Bukkit.getPlayer(active.getOwner());
                if (owner != null) {
                    VisualAction.send(owner, config, "generator-break-options", new Placeholder()
                            .add("{gen}", active.getGenerator().displayName())
                            .add("{current}", this.generatorManager.getGeneratorCount(owner))
                            .add("{max}", this.userManager.getMaxSlot(owner)));
                }
                // play click button sound
                this.player.playSound(this.player.getLocation(), XSound.UI_BUTTON_CLICK.get(), 1.0f, 1.0f);

                // update the gui
                int currentPage = this.pagination.currentPage;
                this.pagination = new Pagination<>(this.generatorManager.getActiveGenerator(this.user.getUniqueId()), slots);
                if (this.pagination.getItems(currentPage) != null) {
                    this.pagination.currentPage = currentPage;
                }

                this.setAllItems();
            });
        }

        if (config.isConfigurationSection("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                ItemStack stack = ItemBuilder.fromConfig(config, "items." + key).build();
                String type = config.getString("items." + key + ".type", "dummy");
                List<String> commands = config.getStringList("items." + key + ".commands");
                List<Integer> itemSlots = config.getIntegerList("items." + key + ".slots");
                // set the item
                this.setItems(Utils.convertListToIntArray(itemSlots), stack, event -> {
                    // execute commands
                    commands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                            .replace("{player}", this.player.getName())));

                    // check the type
                    if (type.equalsIgnoreCase("next_page")) {
                        if (this.pagination == null || !this.pagination.hasNextPage()) return;
                        this.pagination.currentPage++;
                        this.setAllItems();
                        return;
                    }

                    if (type.equalsIgnoreCase("previous_page")) {
                        if (this.pagination == null || !this.pagination.hasNextPage()) return;
                        this.pagination.currentPage--;
                        this.setAllItems();
                    }

                });
            }
        }

    }

}
