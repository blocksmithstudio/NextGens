package com.muhammaddaffa.nextgens.users;

import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.users.models.User;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserManager {

    private final Map<UUID, User> userMap = new ConcurrentHashMap<>();

    @Nullable
    public User getUser(String name) {
        return userMap.values().stream()
                .filter(user -> user.getName() != null && user.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public User getUser(Player player) {
        return getUser(player.getUniqueId());
    }

    @NotNull
    public User getUser(UUID uuid) {
        return userMap.computeIfAbsent(uuid, User::new);
    }

    public Collection<User> getUsers() {
        return userMap.values();
    }

    public List<String> getUsersName() {
        return userMap.values().stream()
                .map(User::getName)
                .collect(Collectors.toList());
    }

    public List<String> getUsersMemberName(Player player) {
        User user = this.getUser(player);
        return user.getMemberNames();
    }

    public List<String> getWhoUserAddedToPlayer(Player player) {
        return this.userMap.values().stream()
                .filter(user -> user.isMember(player.getUniqueId()))
                .map(User::getName)
                .collect(Collectors.toList());
    }

    public void addUser(User user) {
        userMap.put(user.getUniqueId(), user);
    }

    public void removeUser(UUID uuid) {
        userMap.remove(uuid);
    }

    public int getMaxSlot(Player player) {
        int max = 0;
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (config.getBoolean("default-max-generator.enabled")) {
            max += config.getInt("default-max-generator.amount");
        }
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String permission = pai.getPermission();
            if (!permission.startsWith("nextgens.max.")) {
                continue;
            }
            int current = Integer.parseInt(permission.split("\\.")[2]);
            if (current > max) {
                max = current;
            }
        }

        int bonusMax = max + this.getUser(player).getBonus();
        int limit = config.getInt("player-generator-limit.limit");
        if (config.getBoolean("player-generator-limit.enabled") && bonusMax > limit) {
            return limit;
        }
        return bonusMax;
    }

}
