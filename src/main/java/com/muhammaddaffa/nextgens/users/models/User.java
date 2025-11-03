package com.muhammaddaffa.nextgens.users.models;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class User {

    private final UUID uuid;
    private int bonus;
    // multiplier system
    private double multiplier;
    // statistics
    private double earnings;
    private int itemsSold;
    private int normalSell, sellwandSell;
    // settings
    private boolean toggleCashback = true;
    private boolean toggleInventoryAutoSell = false;
    private boolean toggleGensAutoSell = false;

    private int interval;

    // member
    private final Set<UUID> memberSet = ConcurrentHashMap.newKeySet();
    private final Map<User, Long> invitationMap = new ConcurrentHashMap<>();

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public User(UUID uuid, int bonus, double multiplier, double earnings, int itemsSold, int normalSell, int sellwandSell,
                boolean toggleCashback, boolean toggleInventoryAutoSell, boolean toggleGensAutoSell, Set<UUID> memberSet) {
        this.uuid = uuid;
        this.bonus = bonus;
        this.multiplier = multiplier;
        this.earnings = earnings;
        this.itemsSold = itemsSold;
        this.normalSell = normalSell;
        this.sellwandSell = sellwandSell;
        this.toggleCashback = toggleCashback;
        this.toggleInventoryAutoSell = toggleInventoryAutoSell;
        this.toggleGensAutoSell = toggleGensAutoSell;
        this.memberSet.addAll(memberSet);
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return Bukkit.getOfflinePlayer(this.uuid).getName();
    }

    public int getBonus() {
        return bonus;
    }

    public void setBonus(int bonus) {
        this.bonus = bonus;
        if (this.bonus < 0) {
            this.bonus = 0;
        }
    }

    public void addBonus(int amount) {
        this.bonus += amount;
    }

    public void removeBonus(int amount) {
        this.bonus -= amount;
        if (this.bonus < 0) {
            this.bonus = 0;
        }
    }

    public double getMultiplier() {
        return multiplier;
    }

    public double getVisualMultiplier() {
        double visual = multiplier - 1;
        if (visual < 0) visual = 0;
        return visual;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void addMultiplier(double amount) {
        this.multiplier += amount;
    }

    public void removeMultiplier(double amount) {
        this.multiplier -= amount;
    }

    public double getEarnings() {
        return earnings;
    }

    public void setEarnings(double earnings) {
        this.earnings = earnings;
    }

    public void addEarnings(double amount) {
        this.earnings += amount;
    }

    public void removeEarnings(double amount) {
        this.earnings -= amount;
    }

    public int getItemsSold() {
        return itemsSold;
    }

    public void setItemsSold(int itemsSold) {
        this.itemsSold = itemsSold;
    }

    public void addItemsSold(int amount) {
        this.itemsSold += amount;
    }

    public void removeItemsSold(int amount) {
        this.itemsSold -= amount;
    }

    public int getNormalSell() {
        return normalSell;
    }

    public void setNormalSell(int normalSell) {
        this.normalSell = normalSell;
    }

    public void addNormalSell(int amount) {
        this.normalSell += amount;
    }

    public void removeNormalSell(int amount) {
        this.normalSell -= amount;
    }

    public int getSellwandSell() {
        return sellwandSell;
    }

    public void setSellwandSell(int sellwandSell) {
        this.sellwandSell = sellwandSell;
    }

    public void addSellwandSell(int amount) {
        this.sellwandSell += amount;
    }

    public void removeSellwandSell(int amount) {
        this.sellwandSell -= amount;
    }

    public int getTotalSell() {
        return this.normalSell + this.sellwandSell;
    }

    public boolean isToggleCashback() {
        return toggleCashback;
    }

    public void setToggleCashback(boolean toggleCashback) {
        this.toggleCashback = toggleCashback;
    }

    public boolean isToggleInventoryAutoSell() {
        return toggleInventoryAutoSell;
    }

    public void setToggleInventoryAutoSell(boolean toggleInventoryAutoSell) {
        this.toggleInventoryAutoSell = toggleInventoryAutoSell;
    }

    public boolean isToggleGensAutoSell() {
        return toggleGensAutoSell;
    }

    public void setToggleGensAutoSell(boolean toggleGensAutoSell) {
        this.toggleGensAutoSell = toggleGensAutoSell;
    }

    public int getInterval() {
        return interval;
    }

    public void updateInterval(int amount) {
        this.interval += amount;
    }

    public void setInterval(int amount) {
        this.interval = interval;
    }

    public Set<UUID> getMemberSet() {
        return memberSet;
    }

    public List<String> getMemberNames() {
        return this.memberSet.stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .collect(Collectors.toList());
    }

    public boolean isMember(UUID uuid) {
        if (uuid == null) return false;
        return this.memberSet.contains(uuid);
    }

    public boolean isMember(String playerName) {
        for (String member : getMemberNames()) {
            if (member.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public void addMember(UUID uuid) {
        this.memberSet.add(uuid);
    }

    public void removeMember(UUID uuid) {
        this.memberSet.remove(uuid);
    }

    public void removeMember(String playerName) {
        for (UUID id : memberSet) {
            String name = Bukkit.getOfflinePlayer(id).getName();
            if (name != null) {
                if (name.equalsIgnoreCase(playerName)) {
                    memberSet.remove(id);
                }
            }
        }
    }

    public void clearMember() {
        this.memberSet.clear();
    }

    public Map<User, Long> getInvitationMap() {
        return invitationMap;
    }

    public List<User> getValidInvitation() {
        List<User> validInvitation = new ArrayList<>();
        // Filter the invitation
        this.invitationMap.forEach((user, elapsed) -> {
            // Get the elapsed time in seconds
            long elapsedInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - elapsed);
            if (elapsedInSeconds > 60) {
                this.invitationMap.remove(user);
                return;
            }
            validInvitation.add(user);
        });
        return validInvitation;
    }

    public List<String> getInvitationNames() {
        return this.getValidInvitation().stream()
                .map(User::getName)
                .collect(Collectors.toList());
    }

    public boolean hasInvitation(User user) {
        return this.getValidInvitation().stream()
                .anyMatch(u -> u.getUniqueId().equals(user.getUniqueId()));
    }

    public void addInvitation(User user) {
        this.invitationMap.put(user, System.currentTimeMillis());
    }

    public void removeInvitation(User user) {
        this.invitationMap.remove(user);
    }



}
