package com.muhammaddaffa.nextgens.sell.multipliers.providers;

import com.muhammaddaffa.nextgens.sell.multipliers.SellMultiplierProvider;
import com.muhammaddaffa.nextgens.sellwand.models.SellwandData;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PermissionSellMultiplierProvider implements SellMultiplierProvider {

    @Override
    public double getMultiplier(Player player, User user, SellwandData sellwand) {
        return this.getSellMultiplier(player);
    }

    private double getSellMultiplier(Player player) {
        if (player == null) return 0;

        double multiplier = 0.0;
        String prefix = "nextgens.multiplier.sell.";
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String permission = pai.getPermission();
            if (!pai.getValue() || !permission.startsWith(prefix)) {
                continue;
            }
            try {
                String split = permission.substring(prefix.length()).replace("_", ".");
                double current = Double.parseDouble(split);
                if (Double.isFinite(current) && current > multiplier) {
                    multiplier = current;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        // get the multiplier in decimals
        return multiplier;
    }

}
