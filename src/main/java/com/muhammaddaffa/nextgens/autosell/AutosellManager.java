package com.muhammaddaffa.nextgens.autosell;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.sell.SellManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public record AutosellManager(
        UserManager userManager
) {

    public void startTask() {
        ExecutorManager.getProvider().syncTimer(1L, 20L, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = this.userManager.getUser(player);
                // check if player has autosell inventory permission
                if (!user.isToggleInventoryAutoSell()) continue;
                // get the interval
                int sellInterval = NextGens.DEFAULT_CONFIG.getConfig().getInt("autosell.inventory-interval");
                // check if sell interval is equals or greater than user interval
                if (sellInterval >= user.getInterval()) {
                    // sell the inventory
                    NextGens.getInstance().getSellManager().performSell(player, null, true, player.getInventory());
                    // set interval back to 0
                    user.setInterval(0);
                    continue;
                }
                // update the player interval
                user.updateInterval(1);
            }
        });
    }

}
