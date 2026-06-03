package com.muhammaddaffa.nextgens.api.events.sell;

import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.SellData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SellwandUseEvent extends SellEvent{

    public SellwandUseEvent(Player player, User user, SellData sellData, Block block) {
        super(player, user, sellData, block);
    }

}
