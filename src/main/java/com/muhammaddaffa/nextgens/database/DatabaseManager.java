package com.muhammaddaffa.nextgens.database;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseManager {

    public static final String GENERATOR_TABLE = "nextgens_generator";
    public static final String USER_TABLE = "nextgens_user";

    private HikariDataSource dataSource;
    private boolean mysql;
    private final Map<String, GeneratorWrite> pendingGenerators = new LinkedHashMap<>();
    private final Object generatorWriteLock = new Object();
    private boolean generatorWriteScheduled;
    private boolean closing;

    public void connect() {
        // get all variables we want
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        String path = "plugins/NextGens/generators.db";
        // create the hikari config
        HikariConfig hikari = new HikariConfig();
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.setPoolName("NextGens Database Pool");
        hikari.setMaxLifetime(1000000);
        hikari.setConnectionTimeout(30000);
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
        this.queueGenerator(LocationUtils.serialize(active.getLocation()), new GeneratorWrite(null, null, 0, false));
    }

    public void saveGenerator(ActiveGenerator active) {
        if (active == null || active.getOwner() == null || active.getLocation().getWorld() == null) {
            return;
        }
        this.queueGenerator(LocationUtils.serialize(active.getLocation()), new GeneratorWrite(
                active.getOwner().toString(), active.getGenerator().id(), active.getTimer(), active.isCorrupted()));
    }

    public void saveGenerator(Collection<ActiveGenerator> activeGenerators) {
        activeGenerators.forEach(this::saveGenerator);
    }

    private void queueGenerator(String location, GeneratorWrite write) {
        synchronized (this.pendingGenerators) {
            if (this.closing) {
                throw new IllegalStateException("Generator database is closing");
            }
            // Keep only the latest state for each location, including removals and replacements.
            this.pendingGenerators.put(location, write);
            this.scheduleGeneratorWrites(1L);
        }
    }

    private void scheduleGeneratorWrites(long delay) {
        if (this.closing || this.generatorWriteScheduled || this.pendingGenerators.isEmpty()) {
            return;
        }
        this.generatorWriteScheduled = true;
        try {
            ExecutorManager.getProvider().asyncLater(delay, this::flushGeneratorWrites);
        } catch (RuntimeException ex) {
            this.generatorWriteScheduled = false;
            throw ex;
        }
    }

    private void flushGeneratorWrites() {
        // Shutdown and the async writer must never execute batches out of order.
        synchronized (this.generatorWriteLock) {
            try {
                while (true) {
                    Map<String, GeneratorWrite> batch = new LinkedHashMap<>();
                    synchronized (this.pendingGenerators) {
                        java.util.Iterator<Map.Entry<String, GeneratorWrite>> iterator = this.pendingGenerators.entrySet().iterator();
                        while (iterator.hasNext() && batch.size() < 100) {
                            Map.Entry<String, GeneratorWrite> entry = iterator.next();
                            batch.put(entry.getKey(), entry.getValue());
                            iterator.remove();
                        }
                    }
                    if (batch.isEmpty()) {
                        return;
                    }
                    try {
                        this.writeGeneratorBatch(batch);
                    } catch (SQLException ex) {
                        synchronized (this.pendingGenerators) {
                            // A newer queued state takes precedence over a failed older write.
                            batch.forEach(this.pendingGenerators::putIfAbsent);
                        }
                        Logger.severe("Failed to save generator changes; retaining pending writes for retry.");
                        ex.printStackTrace();
                        return;
                    }
                }
            } finally {
                synchronized (this.pendingGenerators) {
                    this.generatorWriteScheduled = false;
                    this.scheduleGeneratorWrites(100L);
                }
            }
        }
    }

    private void writeGeneratorBatch(Map<String, GeneratorWrite> batch) throws SQLException {
        String saveQuery = this.mysql ?
                "INSERT INTO " + GENERATOR_TABLE + " " +
                        "(owner, location, generator_id, timer, is_corrupted) " +
                        "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE owner = VALUES(owner), generator_id = VALUES(generator_id), timer = VALUES(timer), is_corrupted = VALUES(is_corrupted)" :
                "INSERT INTO " + GENERATOR_TABLE + " " +
                        "(owner, location, generator_id, timer, is_corrupted) " +
                        "VALUES (?,?,?,?,?) ON CONFLICT(location) DO UPDATE SET owner = excluded.owner, generator_id = excluded.generator_id, timer = excluded.timer, is_corrupted = excluded.is_corrupted";
        String deleteQuery = "DELETE FROM " + GENERATOR_TABLE + " WHERE location=?";

        try (Connection connection = this.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement save = connection.prepareStatement(saveQuery);
                 PreparedStatement delete = connection.prepareStatement(deleteQuery)) {
                for (Map.Entry<String, GeneratorWrite> entry : batch.entrySet()) {
                    GeneratorWrite write = entry.getValue();
                    if (write.owner() == null) {
                        delete.setString(1, entry.getKey());
                        delete.addBatch();
                    } else {
                        save.setString(1, write.owner());
                        save.setString(2, entry.getKey());
                        save.setString(3, write.generatorId());
                        save.setDouble(4, write.timer());
                        save.setBoolean(5, write.corrupted());
                        save.addBatch();
                    }
                }
                delete.executeBatch();
                save.executeBatch();
                connection.commit();
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
                throw ex;
            }
        }
    }

    private record GeneratorWrite(String owner, String generatorId, double timer, boolean corrupted) {
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
        synchronized (this.pendingGenerators) {
            this.closing = true;
        }
        this.flushGeneratorWrites();
        synchronized (this.pendingGenerators) {
            if (!this.pendingGenerators.isEmpty()) {
                Logger.severe("Database closed with " + this.pendingGenerators.size()
                        + " unsaved generator changes; check database connectivity before restarting.");
            }
        }
        if (this.dataSource != null) {
            this.dataSource.close();
        }
    }

    public interface QueryConsumer<T> {

        void accept(T value) throws SQLException;

    }

}
