package com.muhammaddaffa.nextgens.autosell;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public record AutosellManager(
        UserManager userManager
) {

    public void startTask() {
        ExecutorManager.getProvider().syncTimer(20L, 20L, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = this.userManager.getUser(player);
                if (!user.isToggleInventoryAutoSell() || !Autosell.hasAutosellInventoryPermission(player)) {
                    user.setInterval(0);
                    continue;
                }
                int sellInterval = Math.max(1, NextGens.DEFAULT_CONFIG.getConfig().getInt("autosell.inventory-interval"));
                user.updateInterval(1);
                if (user.getInterval() >= sellInterval) {
                    NextGens.getInstance().getSellManager().performSell(player, null, true, player.getInventory());
                    user.setInterval(0);
                }
            }
        });
    }

}
