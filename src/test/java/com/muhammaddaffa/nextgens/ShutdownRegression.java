package com.muhammaddaffa.nextgens;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.database.DatabaseManager;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import sun.misc.Unsafe;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/** Exercises actual disable ordering, including failing final database writes. */
public final class ShutdownRegression {
    private static int checks;
    private static final List<String> order = new ArrayList<>();
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static void field(Class<?> type, Object owner, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
    private static void available(String step) {
        check(MDLib.instance() != null, "MDLib must remain initialized during " + step);
        order.add(step);
    }
    private static final class Refunds extends RefundManager {
        Refunds() { super(null); }
        @Override public synchronized void stop() { available("stop-refunds"); }
        @Override public synchronized void saveAll() { available("save-refunds"); }
    }
    private static final class Events extends EventManager {
        boolean fail;
        @Override public void save() {
            available("save-events");
            if (fail) throw new IllegalStateException("Fixture event save failure");
        }
    }
    private static final class Database extends DatabaseManager {
        boolean failClose, completed;
        @Override public Connection getConnection() throws SQLException { throw new SQLException("Fixture final write failure"); }
        @Override public void close() {
            available("close-database");
            Logger.info("Fixture database cleanup still has a working logger");
            if (failClose) throw new IllegalStateException("Fixture database close failure");
            super.close();
            completed = true;
        }
    }
    @SuppressWarnings("unchecked")
    private static void pendingWrite(Database database) throws Exception {
        Field pending = DatabaseManager.class.getDeclaredField("pendingGenerators"); pending.setAccessible(true);
        Class<?> type = Class.forName(DatabaseManager.class.getName() + "$GeneratorWrite");
        Constructor<?> constructor = type.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        ((Map<String, Object>) pending.get(database)).put("fixture", constructor.newInstance("fixture-owner", "fixture-generator", 0d, false));
    }
    public static void main(String[] args) throws Exception {
        Field singleton = Unsafe.class.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        Unsafe unsafe = (Unsafe) singleton.get(null);
        java.util.logging.Logger logging = java.util.logging.Logger.getLogger("shutdown-regression");
        logging.setLevel(java.util.logging.Level.OFF);
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class[]{Server.class},
                (p, m, a) -> m.getName().equals("getLogger") ? logging : null);
        field(Bukkit.class, null, "server", server);
        for (int scenario = 0; scenario < 4; scenario++) {
            order.clear();
            NextGens plugin = (NextGens) unsafe.allocateInstance(NextGens.class);
            field(JavaPlugin.class, plugin, "server", server);
            field(JavaPlugin.class, plugin, "description", new PluginDescriptionFile("ShutdownRegression", "1.0", "fixture.Main"));
            field(JavaPlugin.class, plugin, "logger", new PluginLogger(plugin));
            field(MDLib.class, null, "instance", plugin);
            Events events = new Events(); events.fail = scenario == 2;
            Database database = new Database(); database.failClose = scenario == 3;
            if (scenario == 1) pendingWrite(database);
            field(NextGens.class, plugin, "refundManager", new Refunds());
            field(NextGens.class, plugin, "eventManager", events);
            field(NextGens.class, plugin, "dbm", database);
            boolean threw = false;
            try { plugin.onDisable(); } catch (IllegalStateException expected) { threw = true; }
            check(threw == (scenario >= 2), "Only the configured fixture failure propagates");
            check(MDLib.instance() == null, "MDLib always shuts down after persistence");
            check(order.equals(List.of("stop-refunds", "save-refunds", "save-events", "close-database")), "Database close runs after saves, including when a save fails");
            check(database.completed == (scenario != 3), "Database cleanup completes even if its final write or an earlier save fails");
        }
        System.out.println("PASS: " + checks + " shutdown regression checks");
    }
}
