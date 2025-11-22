package com.muhammaddaffa.nextgens.api;

import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.events.Event;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.worth.WorthManager;
import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GeneratorAPI {

    private final GeneratorManager generatorManager;
    private final RefundManager refundManager;
    private final UserManager userManager;
    private final WorthManager worthManager;
    private final SellwandManager sellwandManager;
    private final EventManager eventManager;

    public GeneratorAPI(GeneratorManager generatorManager, RefundManager refundManager, UserManager userManager,
                        WorthManager worthManager, SellwandManager sellwandManager, EventManager eventManager) {
        this.generatorManager = generatorManager;
        this.refundManager = refundManager;
        this.userManager = userManager;
        this.worthManager = worthManager;
        this.sellwandManager = sellwandManager;
        this.eventManager = eventManager;
    }

    @Nullable
    public Generator getGenerator(@NotNull String id) {
        return this.generatorManager.getGenerator(id);
    }

    @Nullable
    public Generator getGenerator(@NotNull ItemStack stack) {
        return this.generatorManager.getGenerator(stack);
    }

    public Collection<ActiveGenerator> getActiveGenerator() {
        return this.generatorManager.getActiveGenerator();
    }

    /*@NotNull
    public List<ActiveGenerator> getActiveGenerator(@NotNull Player player) {
        return this.getActiveGenerator(player.getUniqueId());
    }*/

    @NotNull
    public List<ActiveGenerator> getActiveGenerator(@NotNull UUID uuid) {
        return this.getActiveGenerator().stream()
                .filter(active -> active.getOwner().equals(uuid))
                .collect(Collectors.toList());
    }

    @Nullable
    public ActiveGenerator getActiveGenerator(@Nullable Block block) {
        if (block == null) return null;
        return this.getActiveGenerator(block.getLocation());
    }

    @Nullable
    public ActiveGenerator getActiveGenerator(@NotNull Location location) {
        return this.generatorManager.getActiveGenerator(location);
    }

    public void unregisterGenerator(@NotNull Block block) {
        this.unregisterGenerator(block.getLocation());
    }

    public void unregisterGenerator(@NotNull Location location) {
        this.generatorManager.unregisterGenerator(location);
    }

    public void giveGenerator(@NotNull Player player, @NotNull String id) {
        this.giveGenerator(player.getUniqueId(), id);
    }

    public void giveGenerator(@NotNull OfflinePlayer player, @NotNull String id) {
        this.giveGenerator(player.getUniqueId(), id);
    }

    public void giveGenerator(@NotNull UUID uuid, @NotNull String id) {
        this.refundManager.delayedGiveGeneratorItem(uuid, id);
    }

    @NotNull
    public User getUser(@NotNull Player player) {
        return this.getUser(player.getUniqueId());
    }

    @NotNull
    public User getUser(@NotNull UUID uuid) {
        return this.userManager.getUser(uuid);
    }

    public int getGeneratorLimit(@NotNull Player player) {
        return this.userManager.getMaxSlot(player);
    }

    public int getGeneratorBonusPlace(@NotNull Player player) {
        return this.getGeneratorBonusPlace(player.getUniqueId());
    }

    public int getGeneratorBonusPlace(@NotNull UUID uuid) {
        return this.getUser(uuid).getBonus();
    }

    public int getGeneratorCurrentPlaced(@NotNull Player player) {
        return this.getGeneratorCurrentPlaced(player.getUniqueId());
    }

    public int getGeneratorCurrentPlaced(@NotNull UUID uuid) {
        return this.generatorManager.getGeneratorCount(uuid);
    }

    @Nullable
    private Double getMaterialWorth(@NotNull ItemStack stack) {
        return this.worthManager.getMaterialWorth(stack);
    }

    @Nullable
    private Double getItemWorth(@NotNull ItemStack stack) {
        return this.worthManager.getItemWorth(stack);
    }

    @Nullable
    private Double getItemMetaWorth(@NotNull ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        // check if meta has a value
        if (meta != null) {
            // get the price value
            Double price = meta.getPersistentDataContainer().get(NextGens.drop_value, PersistentDataType.DOUBLE);
            // if price doesn't exist, return null
            if (price == null) {
                return null;
            }
            // return the drop value
            return price * stack.getAmount();
        }
        return null;
    }

    @Nullable
    private Double getShopGuiPlusWorth(@NotNull ItemStack stack) {
        if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("sell-options.hook_shopguiplus") && this.isShopGUIPlus()) {
            // get the price from shopgui+
            double price = ShopGuiPlusApi.getItemStackPriceSell(stack);
            // if price below or equals to 0, make it null
            if (price <= 0) {
                return null;
            }
            return price;
        }
        return null;
    }

    @Nullable
    public Double getWorth(@Nullable ItemStack stack) {
        if (stack == null) return null;
        boolean external = this.isShopGUIPlus();
        Double materialWorth = this.getMaterialWorth(stack);
        Double itemWorth = this.getItemWorth(stack);
        Double itemMetaWorth = this.getItemMetaWorth(stack);
        Double shopGuiPlusWorth = this.getShopGuiPlusWorth(stack);
        // Check if it's generator item
        if (this.generatorManager.isGeneratorItem(stack)) {
            return null;
        }
        // prioritize the shopguiplus first
        if (external && shopGuiPlusWorth != null && itemMetaWorth == null) {
            return shopGuiPlusWorth;
        }
        // next, is the item with item meta
        if (itemMetaWorth != null) {
            return itemMetaWorth;
        }
        // next, is the custom item
        if (itemWorth != null) {
            return itemWorth;
        }
        // finally, the material worth
        return materialWorth;
    }

    @NotNull
    public ItemStack createSellwand(double multiplier, int uses) {
        return this.sellwandManager.create(multiplier, uses);
    }

    @Nullable
    public Event getActiveEvent() {
        return this.eventManager.getActiveEvent();
    }

    @Nullable
    public Event getEvent(String id) {
        return this.eventManager.getEvent(id);
    }

    @NotNull
    public Event getRandomEvent() {
        return this.eventManager.getRandomEvent();
    }

    @NotNull
    public List<Event> getEvents() {
        return this.eventManager.getEvents();
    }

    public void updateSellwand(@Nullable ItemStack stack) {
        this.sellwandManager.update(stack);
    }

    private boolean isShopGUIPlus() {
        return Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null;
    }

}
