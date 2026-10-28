package com.muhammaddaffa.nextgens.autosell;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class Autosell {

    /**
     * Permissions:
     * nextgens.autosell.inv.(interval in seconds)
     * nextgens.autosell.gens
     */

    public static int getAutosellInventoryInterval(Player player) {
        int interval = 0;
        String prefix = "nextgens.autosell.inv.";
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String permission = pai.getPermission();
            if (!pai.getValue() || !permission.startsWith(prefix)) continue;
            try {
                int permissionInterval = Integer.parseInt(permission.substring(prefix.length()));
                if (permissionInterval > 0 && (interval == 0 || permissionInterval < interval)) {
                    interval = permissionInterval;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return interval;
    }

    public static boolean hasAutosellInventoryPermission(Player player) {
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (pai.getValue() && pai.getPermission().equals("nextgens.autosell.inv")) return true;
        }
        return getAutosellInventoryInterval(player) > 0;
    }

    public static boolean hasAutosellGensPermission(Player player) {
        return player.hasPermission("nextgens.autosell.gens");
    }

}
