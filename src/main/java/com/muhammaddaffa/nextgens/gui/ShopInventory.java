package com.muhammaddaffa.nextgens.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import java.util.stream.Collectors;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopInventory extends FastInv {

    private HeadDatabaseAPI hdbAPI;

    public static void openInventory(Player player, GeneratorManager generatorManager) {
        ShopInventory gui = new ShopInventory(player, generatorManager);
        // open the gui
        gui.open(player);
    }

    private final Map<Integer, List<String>> paginationMap = new HashMap<>();

    private final Player player;
    private final GeneratorManager generatorManager;

    private int guiPage = 1;

    public ShopInventory(Player player, GeneratorManager generatorManager) {
        super(NextGens.SHOP_CONFIG.getInt("size"), Common.color(NextGens.SHOP_CONFIG.getString("title")));
        this.player = player;
        this.generatorManager = generatorManager;

        if (Bukkit.getPluginManager().getPlugin("HeadDatabase") != null) {
            this.hdbAPI = new HeadDatabaseAPI();
        }

        this.loadItems();
        this.setAllItems();
    }

    private ItemStack createItemFromConfig(FileConfiguration config, String key) {
        String materialString = config.getString(key + ".material", "STONE");

        if (materialString.startsWith("hdb-") && hdbAPI != null) {
            try {
                String headId = materialString.substring(4);
                ItemStack head = hdbAPI.getItemHead(headId);

                if (head != null) {
                    ItemBuilder builder = new ItemBuilder(head);

                    if (config.contains(key + ".display-name")) {
                        String displayName = config.getString(key + ".display-name");
                        builder.name(Common.color(displayName));
                    }

                    if (config.contains(key + ".lore")) {
                        List<String> lore = config.getStringList(key + ".lore");
                        List<String> coloredLore = lore.stream()
                                .map(Common::color)
                                .collect(Collectors.toList());
                        builder.lore(coloredLore);
                    }


                    return builder.build();
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to get HeadDatabase head with ID: " + materialString);
                e.printStackTrace();
            }
        }
        ItemBuilder builder = ItemBuilder.fromConfig(config, key);
        return builder != null ? builder.build() : new ItemStack(Material.STONE);
    }


    private void loadItems() {
        // clear the items first
        this.paginationMap.clear();
        // load the config
        FileConfiguration config = NextGens.SHOP_CONFIG.getConfig();
        // load the items first
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            int page = config.getInt("items." + key + ".page", 1);
            // add the item into the page
            List<String> pageItems = this.paginationMap.computeIfAbsent(page, k -> new ArrayList<>());
            pageItems.add("items." + key);
        }
    }

    private void setAllItems() {
        // clear the gui first
        for (int i = 0; i < this.getInventory().getSize(); i++) {
            this.setItem(i, new ItemStack(Material.AIR));
        }
        // proceed to set all items
        FileConfiguration config = NextGens.SHOP_CONFIG.getConfig();
        // get the items according to the page
        List<String> pageItems = this.paginationMap.get(this.guiPage);
        if (pageItems == null) {
            return;
        }
        // loop through the items
        for (String key : pageItems) {
            // get the data
            String type = config.getString(key + ".type", "dummy");
            List<Integer> slots = config.getIntegerList(key + ".slots");
            List<String> commands = config.getStringList(key + ".commands");
            ItemStack stack = createItemFromConfig(config, key);
            if (stack == null) continue;

            if (type.equalsIgnoreCase("GENERATOR")) {
                String id = config.getString(key + ".generator");
                Generator generator = this.generatorManager.getGenerator(id);
                double cost = config.getDouble(key + ".cost");

                // skip if the generator is invalid
                if (generator == null) {
                    continue;
                }

                this.setItems(Utils.convertListToIntArray(slots), stack, event -> {
                    // money check
                    if (VaultEconomy.getBalance(this.player) < cost) {
                        NextGens.DEFAULT_CONFIG.sendMessage(this.player, "messages.not-enough-money", new Placeholder()
                                .add("{money}", Common.digits(VaultEconomy.getBalance(this.player)))
                                .add("{upgradecost}", Common.digits(cost))
                                .add("{remaining}", Common.digits(VaultEconomy.getBalance(this.player) - cost)));
                        // play bass sound
                        Utils.bassSound(this.player);
                        // close on no money
                        if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("close-on-no-money")) {
                            this.player.closeInventory();
                        }
                        return;
                    }
                    // reduce the amount
                    VaultEconomy.withdraw(this.player, cost);
                    // give the generator
                    Common.addInventoryItem(this.player, generator.createItem(1));
                    // send message
                    NextGens.DEFAULT_CONFIG.sendMessage(this.player, "messages.gen-purchase", new Placeholder()
                            .add("{gen}", generator.displayName())
                            .add("{cost}", Common.digits(cost)));
                    // close on purchase
                    if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("close-on-purchase")) {
                        this.player.closeInventory();
                    }
                });
                continue;
            }
            if (type.equalsIgnoreCase("JUMP_TO_PAGE")) {
                int targetPage = config.getInt(key + ".target_page", 1);
                this.setItems(Utils.convertListToIntArray(slots), stack, event -> {
                    if (this.paginationMap.containsKey(targetPage)) {
                        this.guiPage = targetPage;
                        this.setAllItems();
                    }
                });
                continue;
            }
            if (type.equalsIgnoreCase("NEXT_PAGE")) {
                this.setItems(Utils.convertListToIntArray(slots), stack, event -> {
                    if (this.paginationMap.containsKey(this.guiPage + 1)) {
                        this.guiPage += 1;
                        this.setAllItems();
                    }
                });
                continue;
            }

            if (type.equalsIgnoreCase("PREVIOUS_PAGE")) {
                this.setItems(Utils.convertListToIntArray(slots), stack, event -> {
                    if (this.paginationMap.containsKey(this.guiPage - 1)) {
                        this.guiPage -= 1;
                        this.setAllItems();
                    }
                });
                continue;
            }

            // set the normal items
            this.setItems(Utils.convertListToIntArray(slots), stack, event -> {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                // Execute the console commands
                commands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                        .replace("{player}", player.getName())));
            });
        }

    }

}
