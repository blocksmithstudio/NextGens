package com.muhammaddaffa.nextgens.users;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.database.DatabaseManager;
import com.muhammaddaffa.nextgens.users.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class UserRepository {

    private final DatabaseManager dbManager;
    private final UserManager userManager;

    public UserRepository(DatabaseManager dbManager, UserManager userManager) {
        this.dbManager = dbManager;
        this.userManager = userManager;
    }

    public void loadUsers() {
        String query = "SELECT * FROM " + DatabaseManager.USER_TABLE;
        dbManager.executeQuery(query, result -> {
            while (result.next()) {
                User user = extractUserFromResultSet(result);
                userManager.addUser(user);
            }
            Logger.info("Successfully loaded " + userManager.getUsers().size() + " users data!");
        });
    }

    public void saveUser(User user) {
        saveUsers(List.of(user));
    }

    public void saveUsers(List<User> users) {
        String query = NextGens.getInstance().getDatabaseManager().isMysql() ?
                "INSERT INTO " + DatabaseManager.USER_TABLE + " " +
                        "(uuid, bonus, multiplier, earnings, items_sold, normal_sell, sellwand_sell, toggle_cashback, " +
                        "toggle_inventory_sell, toggle_gens_sell, member_set) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "bonus = VALUES(bonus), " +
                        "multiplier = VALUES(multiplier), " +
                        "earnings = VALUES(earnings), " +
                        "items_sold = VALUES(items_sold), " +
                        "normal_sell = VALUES(normal_sell), " +
                        "sellwand_sell = VALUES(sellwand_sell), " +
                        "toggle_cashback = VALUES(toggle_cashback), " +
                        "toggle_inventory_sell = VALUES(toggle_inventory_sell), " +
                        "toggle_gens_sell = VALUES(toggle_gens_sell), " +
                        "member_set = VALUES(member_set)" :
                "INSERT INTO " + DatabaseManager.USER_TABLE + " " +
                        "(uuid, bonus, multiplier, earnings, items_sold, normal_sell, sellwand_sell, toggle_cashback, " +
                        "toggle_inventory_sell, toggle_gens_sell, member_set) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET " +
                        "bonus = excluded.bonus, " +
                        "multiplier = excluded.multiplier, " +
                        "earnings = excluded.earnings, " +
                        "items_sold = excluded.items_sold, " +
                        "normal_sell = excluded.normal_sell, " +
                        "sellwand_sell = excluded.sellwand_sell, " +
                        "toggle_cashback = excluded.toggle_cashback, " +
                        "toggle_inventory_sell = excluded.toggle_inventory_sell, " +
                        "toggle_gens_sell = excluded.toggle_gens_sell, " +
                        "member_set = excluded.member_set";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (User user : users) {
                setStatementParameters(statement, user);
                statement.addBatch();
            }
            statement.executeBatch();
            Logger.info("Successfully saved " + users.size() + " users data!");
        } catch (SQLException ex) {
            Logger.severe("Failed to save " + users.size() + " users data!");
            ex.printStackTrace();
        }
    }

    private User extractUserFromResultSet(ResultSet result) throws SQLException {
        UUID uuid = UUID.fromString(result.getString(1));
        int bonus = result.getInt(2);
        double multiplier = result.getDouble(3);
        double earnings = result.getDouble(4);
        int itemsSold = result.getInt(5);
        int normalSell = result.getInt(6);
        int sellwandSell = result.getInt(7);
        boolean toggleCashback = result.getBoolean(8);
        boolean toggleInventorySell = result.getBoolean(9);
        boolean toggleGensSell = result.getBoolean(10);
        Set<UUID> memberSet = result.getString(11) != null ?
                Arrays.stream(result.getString(11).split(";"))
                        .map(UUID::fromString)
                        .collect(Collectors.toSet()) : new HashSet<>();

        return new User(uuid, bonus, multiplier, earnings, itemsSold, normalSell,
                sellwandSell, toggleCashback, toggleInventorySell, toggleGensSell, memberSet);
    }

    private void setStatementParameters(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getUniqueId().toString());
        statement.setInt(2, user.getBonus());
        statement.setDouble(3, user.getMultiplier());
        statement.setDouble(4, user.getEarnings());
        statement.setInt(5, user.getItemsSold());
        statement.setInt(6, user.getNormalSell());
        statement.setInt(7, user.getSellwandSell());
        statement.setBoolean(8, user.isToggleCashback());
        statement.setBoolean(9, user.isToggleInventoryAutoSell());
        statement.setBoolean(10, user.isToggleGensAutoSell());
        statement.setString(11, user.getMemberSet().stream().map(UUID::toString).collect(Collectors.joining(";")));
    }

}
