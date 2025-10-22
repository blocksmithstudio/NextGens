package com.muhammaddaffa.nextgens.database;

import com.muhammaddaffa.mdlib.utils.LocationUtils;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Consumer;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.List;

public class DatabaseManager {

    public static final String GENERATOR_TABLE = "nextgens_generator";
    public static final String USER_TABLE = "nextgens_user";

    private HikariDataSource dataSource;
    private boolean mysql;

    public void connect() {
        // get all variables we want
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        String path = "plugins/NextGens/generators.db";
        // create the hikari config
        HikariConfig hikari = new HikariConfig();
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.setPoolName("NextGens Database Pool");
        hikari.setMaxLifetime(1000000);
        hikari.setConnectionTimeout(600000);
        hikari.setIdleTimeout(600000);
        hikari.setLeakDetectionThreshold(360000);
        hikari.addDataSourceProperty("characterEncoding", "utf8");
        hikari.addDataSourceProperty("useUnicode", true);

        if (config.getBoolean("mysql.enabled")) {
            this.mysql = true;
            String host = config.getString("mysql.host");
            int port = config.getInt("mysql.port");
            String database = config.getString("mysql.database");
            String user = config.getString("mysql.user");
            String password = config.getString("mysql.password");
            boolean useSSL = config.getBoolean("mysql.useSSL");
            Logger.info("Trying to connect to the MySQL database...");

            hikari.setDriverClassName("com.mysql.jdbc.Driver");
            hikari.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%b",
                    host, port, database, useSSL));
            hikari.setUsername(user);
            hikari.setPassword(password);
            hikari.setMinimumIdle(5);
            hikari.setMaximumPoolSize(50);

            this.dataSource = new HikariDataSource(hikari);
            Logger.info("Successfully established connection with MySQL database!");

        } else {
            this.mysql = false;
            hikari.setConnectionTestQuery("SELECT 1");
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + path);
            hikari.setMaximumPoolSize(10);

            Logger.info("Trying to connect to the SQLite database...");
            // create the file if it's not exist
            try {
                File file = new File(path);
                if (!file.exists()) {
                    file.createNewFile();
                }
                this.dataSource = new HikariDataSource(hikari);
                Logger.info("Successfully established connection with SQLite database!");
            } catch (IOException ex) {
                Logger.severe("Failed to create the database file, stopping the server!");
                Bukkit.getPluginManager().disablePlugin(NextGens.getInstance());
                throw new RuntimeException(ex);
            }
        }
    }

    public void createGeneratorTable() {
        this.executeUpdate("CREATE TABLE IF NOT EXISTS " + GENERATOR_TABLE + " (" +
                "owner VARCHAR(255), " +
                "location TEXT UNIQUE, " +
                "generator_id TEXT, " +
                "timer DECIMAL(18,2), " +
                "is_corrupted INT" +
                ");");
    }

    public void createUserTable() {
        this.executeUpdate("CREATE TABLE IF NOT EXISTS " + USER_TABLE + " (" +
                "uuid VARCHAR(255) UNIQUE, " +
                "bonus INT, " +
                "multiplier DOUBLE, " +
                "earnings DOUBLE, " +
                "items_sold INTEGER, " +
                "normal_sell INTEGER, " +
                "sellwand_sell INTEGER, " +
                "toggle_cashback BOOL, " +
                "toggle_inventory_sell BOOL, " +
                "toggle_gens_sell BOOL, " +
                "member_set TEXT" +
                ");");
        // add column if not exists
        // directly add the table and ignore the error
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN multiplier DOUBLE NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN earnings DOUBLE NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN items_sold INTEGER NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN normal_sell INTEGER NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN sellwand_sell INTEGER NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN toggle_cashback BOOL NOT NULL DEFAULT 1;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN toggle_inventory_sell BOOL NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN toggle_gens_sell BOOL NOT NULL DEFAULT 0;", ex -> {});
        this.executeUpdate("ALTER TABLE " + USER_TABLE + " ADD COLUMN member_set TEXT", ex -> {});
    }

    public void deleteGenerator(ActiveGenerator active) {
        String query = "DELETE FROM " + GENERATOR_TABLE + " WHERE location=?;";
        try (Connection connection = this.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, LocationUtils.serialize(active.getLocation()));

            statement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void saveGenerator(ActiveGenerator active) {
        // if the world is null, skip it
        if (active == null || active.getOwner() == null || active.getLocation().getWorld() == null) {
            return;
        }
        this.saveGenerator(List.of(active));
    }

    public void saveGenerator(Collection<ActiveGenerator> activeGenerators) {
        String query = this.mysql ?
                "INSERT INTO " + GENERATOR_TABLE + " " +
                        "(owner, location, generator_id, timer, is_corrupted) " +
                        "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE generator_id = VALUES(generator_id), timer = VALUES(timer), is_corrupted = VALUES(is_corrupted)" :
                "INSERT INTO " + GENERATOR_TABLE + " " +
                        "(owner, location, generator_id, timer, is_corrupted) " +
                        "VALUES (?,?,?,?,?) ON CONFLICT(location) DO UPDATE SET generator_id = excluded.generator_id, timer = excluded.timer, is_corrupted = excluded.is_corrupted";

        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            int batchSize = 0;

            for (ActiveGenerator active : activeGenerators) {
                if (active.getOwner() == null || active.getLocation().getWorld() == null) continue;

                statement.setString(1, active.getOwner().toString());
                statement.setString(2, LocationUtils.serialize(active.getLocation()));
                statement.setString(3, active.getGenerator().id());
                statement.setDouble(4, active.getTimer());
                statement.setBoolean(5, active.isCorrupted());

                statement.addBatch();
                batchSize++;

                if (batchSize % 100 == 0) {
                    statement.executeBatch();
                    batchSize = 0;
                }
            }

            // Execute the remaining batch
            statement.executeBatch();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void executeUpdate(String statement) {
        this.executeUpdate(statement, error -> {
            Logger.severe("An error occurred while running statement: " + statement);
            error.printStackTrace();
        });
    }

    public void executeUpdate(String statement, Consumer<SQLException> onFailure) {
        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(statement)) {

            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            onFailure.accept(ex);
        }
    }

    public void executeQuery(String statement, QueryConsumer<ResultSet> callback) {
        this.executeQuery(statement, callback, error -> {
            Logger.severe("An error occurred while running query: " + statement);
            error.printStackTrace();
        });
    }

    public void executeQuery(String statement, QueryConsumer<ResultSet> callback, Consumer<SQLException> onFailure) {
        try (Connection connection = this.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(statement);
                ResultSet result = preparedStatement.executeQuery()) {

            callback.accept(result);
        } catch (SQLException ex) {
            onFailure.accept(ex);
        }
    }

    public void buildStatement(String query, QueryConsumer<PreparedStatement> consumer) {
        this.buildStatement(query, consumer, error -> {
            Logger.severe("An error occurred while building statement: " + query);
            error.printStackTrace();
        });
    }

    public void buildStatement(String query, QueryConsumer<PreparedStatement> consumer, Consumer<SQLException> onFailure) {
        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            consumer.accept(preparedStatement);
        } catch (SQLException ex) {
            onFailure.accept(ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public boolean isMysql() {
        return mysql;
    }

    public void close() {
        this.dataSource.close();
    }

    public interface QueryConsumer<T> {

        void accept(T value) throws SQLException;

    }

}
