package com.muhammaddaffa.nextgens.api.events.sell;

import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.SellData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class SellEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled;

    // --------------------------------------------------------

    private final Player player;
    private final User user;
    private final @Nullable Block block;
    private SellData sellData;

    public SellEvent(Player player, User user, SellData sellData) {
        this(player, user, sellData, null);
    }

    public SellEvent(Player player, User user, SellData sellData, @Nullable Block block) {
        this.player = player;
        this.user = user;
        this.sellData = sellData;
        this.block = block;
    }


    public Player getPlayer() {
        return player;
    }

    public User getUser() {
        return user;
    }

    public SellData getSellData() {
        return sellData;
    }

    public @Nullable Block getBlock() {
        return block;
    }

    public double getMultiplier() {
        return sellData.getMultiplier();
    }

    public void setMultiplier(double multiplier) {
        sellData.setMultiplier(multiplier);
    }

    public void setSellData(SellData sellData) {
        this.sellData = sellData;
    }

    // --------------------------------------------------------

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
