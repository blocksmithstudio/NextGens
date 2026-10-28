import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Run from the project root: java src/test/checks/SellingCheck.java "<Maven test classpath>". */
public class SellingCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the Maven dependency classpath");
        Path output = Files.createTempDirectory(Files.createDirectories(Path.of("target")), "selling-check-");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("com.muhammaddaffa.mdlib.utils.Config", """
                package com.muhammaddaffa.mdlib.utils;
                public class Config {
                    public final org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
                    public org.bukkit.configuration.file.FileConfiguration getConfig() { return config; }
                    public boolean getBoolean(String key) { return config.getBoolean(key); }
                    public int getInt(String key) { return config.getInt(key); }
                    public String getString(String key) { return config.getString(key); }
                    public void sendMessage(org.bukkit.command.CommandSender player, String key, Placeholder... placeholders) { }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.ItemBuilder", """
                package com.muhammaddaffa.mdlib.utils;
                public class ItemBuilder {
                    public static org.bukkit.entity.Player lastPlayer;
                    public static ItemBuilder fromConfig(org.bukkit.entity.Player player, org.bukkit.configuration.file.FileConfiguration config, String key) { lastPlayer = player; return new ItemBuilder(); }
                    public org.bukkit.inventory.ItemStack build() { return new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.fastinv.FastInv", """
                package com.muhammaddaffa.mdlib.fastinv;
                public class FastInv {
                    public static final java.util.List<java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent>> handlers = new java.util.ArrayList<>();
                    private final org.bukkit.inventory.Inventory inventory;
                    public FastInv(int size, String title) {
                        handlers.clear();
                        inventory = (org.bukkit.inventory.Inventory) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{org.bukkit.inventory.Inventory.class}, (p,m,a) -> m.getName().equals("getSize") ? size : null);
                    }
                    public org.bukkit.inventory.Inventory getInventory() { return inventory; }
                    public void open(org.bukkit.entity.Player player) { }
                    public void setItem(int slot, org.bukkit.inventory.ItemStack stack) { }
                    public void setItems(int[] slots, org.bukkit.inventory.ItemStack stack, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) { handlers.add(handler); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.hooks.VaultEconomy", """
                package com.muhammaddaffa.mdlib.hooks;
                import net.milkbowl.vault.economy.EconomyResponse;
                public class VaultEconomy {
                    public static int mode, deposits, withdrawals;
                    public static double amount;
                    public static Runnable callback;
                    public static double getBalance(org.bukkit.OfflinePlayer player) { return 1000; }
                    private static EconomyResponse result(double value) {
                        if (callback != null) callback.run();
                        if (mode == 2) throw new IllegalStateException("Simulated unavailable economy");
                        if (mode == 3) return null;
                        return new EconomyResponse(value, 1000, mode == 1 ? EconomyResponse.ResponseType.FAILURE : EconomyResponse.ResponseType.SUCCESS, "");
                    }
                    public static EconomyResponse deposit(org.bukkit.OfflinePlayer player, double value) { deposits++; amount += value; return result(value); }
                    public static EconomyResponse withdraw(org.bukkit.OfflinePlayer player, double value) { withdrawals++; return result(value); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.task.ExecutorManager", """
                package com.muhammaddaffa.mdlib.task;
                public class ExecutorManager {
                    private static final ExecutorProvider provider = (ExecutorProvider) java.lang.reflect.Proxy.newProxyInstance(ExecutorProvider.class.getClassLoader(), new Class[]{ExecutorProvider.class}, (p,m,a) -> null);
                    public static ExecutorProvider getProvider() { return provider; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.api.GeneratorAPI", """
                package com.muhammaddaffa.nextgens.api;
                public class GeneratorAPI {
                    public Double getWorth(org.bukkit.inventory.ItemStack stack) { return stack == null ? null : (double) stack.getAmount(); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.users.UserManager", """
                package com.muhammaddaffa.nextgens.users;
                public class UserManager {
                    public final com.muhammaddaffa.nextgens.users.models.User user = new com.muhammaddaffa.nextgens.users.models.User(java.util.UUID.randomUUID());
                    public com.muhammaddaffa.nextgens.users.models.User getUser(org.bukkit.entity.Player player) { return user; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.users.UserRepository", """
                package com.muhammaddaffa.nextgens.users;
                public class UserRepository { public void saveUser(com.muhammaddaffa.nextgens.users.models.User user) { } }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.Generator", """
                package com.muhammaddaffa.nextgens.generators;
                public class Generator { public String displayName() { return "Test generator"; } }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.managers.GeneratorManager", """
                package com.muhammaddaffa.nextgens.generators.managers;
                public class GeneratorManager {
                    public com.muhammaddaffa.nextgens.generators.Generator getGenerator(String id) { return new com.muhammaddaffa.nextgens.generators.Generator(); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.refund.RefundManager", """
                package com.muhammaddaffa.nextgens.refund;
                public class RefundManager {
                    public int delivered;
                    public void giveGeneratorItems(org.bukkit.entity.Player player, com.muhammaddaffa.nextgens.generators.Generator generator, int amount) { delivered += amount; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager", """
                package com.muhammaddaffa.nextgens.sellwand.managers;
                public class SellwandManager {
                    public int sales;
                    public boolean isSellwand(org.bukkit.inventory.ItemStack stack) { return stack != null; }
                    public boolean action(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack stack, org.bukkit.block.Block block, org.bukkit.inventory.Inventory... inventories) { sales++; return true; }
                }
                """);
        sources.put("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI", """
                package com.bgsoftware.superiorskyblock.api;
                public class SuperiorSkyblockAPI {
                    public static com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer player;
                    public static com.bgsoftware.superiorskyblock.api.island.Island island;
                    public static com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer getPlayer(org.bukkit.entity.Player ignored) { return player; }
                    public static com.bgsoftware.superiorskyblock.api.island.Island getIslandAt(org.bukkit.Location ignored) { return island; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.Utils", """
                package com.muhammaddaffa.nextgens.utils;
                public class Utils {
                    public static void bassSound(org.bukkit.entity.Player player) { }
                    public static String formatBalance(long value) { return Long.toString(value); }
                    public static int[] convertListToIntArray(java.util.List<Integer> list) { return list.stream().mapToInt(Integer::intValue).toArray(); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.VisualAction", """
                package com.muhammaddaffa.nextgens.utils;
                public class VisualAction {
                    public static void send(org.bukkit.entity.Player player, org.bukkit.configuration.file.FileConfiguration config, String key, com.muhammaddaffa.mdlib.utils.Placeholder placeholder) { }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.NextGens", """
                package com.muhammaddaffa.nextgens;
                import com.muhammaddaffa.nextgens.sell.multipliers.SellMultiplierRegistry;
                import com.muhammaddaffa.nextgens.sell.multipliers.SellMultiplierProvider;
                public class NextGens {
                    public static final com.muhammaddaffa.mdlib.utils.Config DEFAULT_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.mdlib.utils.Config SHOP_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    private static final NextGens instance = new NextGens();
                    public static final com.muhammaddaffa.nextgens.refund.RefundManager refunds = new com.muhammaddaffa.nextgens.refund.RefundManager();
                    public static org.popcraft.bolt.BoltAPI bolt;
                    public static NextGens getInstance() { return instance; }
                    public static com.muhammaddaffa.nextgens.api.GeneratorAPI getApi() { return new com.muhammaddaffa.nextgens.api.GeneratorAPI(); }
                    public SellMultiplierRegistry getMultiplierRegistry() { return new SellMultiplierRegistry() { @Override public java.util.List<SellMultiplierProvider> getMultipliers() { return java.util.List.of(); } }; }
                    public com.muhammaddaffa.nextgens.users.UserRepository getUserRepository() { return new com.muhammaddaffa.nextgens.users.UserRepository(); }
                    public com.muhammaddaffa.nextgens.refund.RefundManager getRefundManager() { return refunds; }
                    public org.popcraft.bolt.BoltAPI getBoltAPI() { return bolt; }
                }
                """);
        sources.put("SellingCases", """
                import com.muhammaddaffa.nextgens.NextGens;
                import com.muhammaddaffa.nextgens.sell.SellManager;
                import com.muhammaddaffa.nextgens.users.UserManager;
                import com.muhammaddaffa.nextgens.api.events.sell.SellEvent;
                import com.muhammaddaffa.nextgens.gui.ShopInventory;
                import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
                import com.muhammaddaffa.nextgens.sellwand.listeners.SellwandListener;
                import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
                import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
                import com.muhammaddaffa.mdlib.fastinv.FastInv;
                import org.bukkit.*;
                import org.bukkit.block.*;
                import org.bukkit.entity.Player;
                import org.bukkit.event.*;
                import org.bukkit.event.block.Action;
                import org.bukkit.event.player.PlayerInteractEvent;
                import org.bukkit.inventory.*;
                import org.bukkit.plugin.PluginManager;
                import java.lang.reflect.*;
                import java.util.*;
                import java.util.function.Consumer;
                import java.util.logging.Logger;

                public class SellingCases {
                    static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
                    static final Set<String> enabled = new HashSet<>();
                    static Consumer<SellEvent> eventHandler = event -> { };
                    static int checks;
                    static <T> T proxy(Class<T> type, InvocationHandler handler) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler)); }
                    static void check(boolean value, String description) { if (!value) throw new AssertionError(description); checks++; }
                    static class Stack extends ItemStack {
                        final String id;
                        int amount;
                        Stack(String id, int amount) { this.id=id; this.amount=amount; }
                        @Override public int getAmount() { return amount; }
                        @Override public void setAmount(int amount) { this.amount=amount; }
                        @Override public Stack clone() { return new Stack(id, amount); }
                        @Override public boolean equals(Object other) { return other instanceof Stack stack && id.equals(stack.id) && amount==stack.amount; }
                        @Override public int hashCode() { return Objects.hash(id,amount); }
                    }
                    static class Slots {
                        final ItemStack[] items = {new Stack("drop",64),null};
                        final Inventory inventory = proxy(Inventory.class,(p,m,a)->switch(m.getName()) {
                            case "getSize" -> items.length;
                            case "getItem" -> items[(Integer)a[0]];
                            case "setItem" -> {items[(Integer)a[0]]=(ItemStack)a[1]; yield null;}
                            case "hashCode" -> System.identityHashCode(p);
                            case "equals" -> p==a[0];
                            default -> null;
                        });
                        boolean intact() { return new Stack("drop",64).equals(items[0]); }
                    }
                    static void reset() { eventHandler=event->{}; VaultEconomy.mode=0; VaultEconomy.deposits=0; VaultEconomy.withdrawals=0; VaultEconomy.amount=0; VaultEconomy.callback=null; }
                    public static void main(String[] args) throws Exception {
                        PluginManager plugins=proxy(PluginManager.class,(p,m,a)->switch(m.getName()) {
                            case "callEvent" -> {eventHandler.accept((SellEvent)a[0]); yield null;}
                            case "isPluginEnabled" -> enabled.contains(a[0]);
                            default -> null;
                        });
                        Bukkit.setServer(proxy(Server.class,(p,m,a)->switch(m.getName()) {
                            case "getPluginManager" -> plugins;
                            case "getLogger" -> Logger.getLogger("selling-check");
                            case "getName","getVersion","getBukkitVersion" -> "selling-check";
                            default -> null;
                        }));
                        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()) {case "getUniqueId"->OWNER;case "getName"->"TestPlayer";default->null;});
                        UserManager users=new UserManager();
                        SellManager manager=new SellManager(users,null);
                        Slots slots=new Slots();
                        eventHandler=event->event.setCancelled(true);
                        check(manager.performSell(player,null,true,slots.inventory)==null && slots.intact() && VaultEconomy.deposits==0,"Cancelled sale must retain inventory and avoid payment");
                        for(int failure=1;failure<=3;failure++) {
                            reset(); VaultEconomy.mode=failure; slots=new Slots();
                            check(manager.performSell(player,null,true,slots.inventory)==null && slots.intact(),"Failed, throwing or null deposit must restore items");
                            check(users.user.getEarnings()==0 && users.user.getItemsSold()==0,"Rejected sale must not update statistics");
                        }
                        reset(); slots=new Slots(); final Slots changed=slots;
                        eventHandler=event->changed.items[0]=new Stack("replacement",5);
                        check(manager.performSell(player,null,true,slots.inventory)==null && VaultEconomy.deposits==0 && slots.items[0].getAmount()==5,"Changed inventory must abort without replacing listener changes");
                        reset(); slots=new Slots(); final Slots recursive=slots;
                        eventHandler=event->check(manager.performSell(player,null,true,recursive.inventory)==null,"Nested overlapping sale must be refused");
                        check(manager.performSell(player,null,true,slots.inventory,slots.inventory)!=null && slots.items[0]==null && VaultEconomy.deposits==1 && VaultEconomy.amount==64,"Success must sell duplicated inventory arguments only once");
                        check(users.user.getEarnings()==64 && users.user.getItemsSold()==64,"Success must update statistics once");
                        reset(); slots=new Slots();
                        check(manager.performSell(player,null,true,slots.inventory)!=null,"Inventory reservation must be released after a completed sale");
                        for(int failure=1;failure<=3;failure++) {
                            reset(); VaultEconomy.mode=failure; Stack drop=new Stack("drop",5);
                            check(!manager.sell(player,drop) && drop.amount==5,"Rejected generator autosell must leave its drop available");
                        }
                        reset(); slots=new Slots(); VaultEconomy.mode=1;
                        check(manager.performSell(player,null,true,slots.inventory)==null && slots.intact(),"Retry setup must preserve the failed sale");
                        VaultEconomy.mode=0;
                        check(manager.performSell(player,null,true,slots.inventory)!=null && slots.items[0]==null,"The same inventory must be sellable after a failed transaction");
                        reset(); Stack drop=new Stack("drop",5);
                        check(manager.sell(player,drop) && drop.amount==0,"Successful generator autosell must consume the drop");

                        var config=NextGens.SHOP_CONFIG.config;
                        config.set("size",9); config.set("title","Test shop");
                        config.set("items.generator.type","GENERATOR"); config.set("items.generator.generator","test");
                        config.set("items.generator.page",1); config.set("items.generator.slots",List.of(0)); config.set("items.generator.cost",25);
                        new ShopInventory(player,new GeneratorManager());
                        for(int failure=1;failure<=3;failure++) {
                            reset(); VaultEconomy.mode=failure;
                            FastInv.handlers.getFirst().accept(null);
                            check(NextGens.refunds.delivered==0,"Failed, throwing or null withdrawal must not give a generator");
                        }
                        reset(); FastInv.handlers.getFirst().accept(null);
                        check(NextGens.refunds.delivered==1 && VaultEconomy.withdrawals==1,"Successful shop withdrawal must give exactly one generator");

                        SellwandManager wands=new SellwandManager(); SellwandListener listener=new SellwandListener(wands);
                        EventHandler annotation=SellwandListener.class.getMethod("onInteract",PlayerInteractEvent.class).getAnnotation(EventHandler.class);
                        check(annotation.priority()==EventPriority.HIGHEST && annotation.ignoreCancelled(),"Protection listeners must run before sellwand consumption");
                        Slots chest=new Slots();
                        Container container=proxy(Container.class,(p,m,a)->m.getName().equals("getInventory")?chest.inventory:null);
                        Block block=proxy(Block.class,(p,m,a)->m.getName().equals("getState")?container:null);
                        PlayerInteractEvent event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,new Stack("wand",1),block,BlockFace.UP,EquipmentSlot.HAND);
                        event.setCancelled(true); listener.onInteract(event);
                        check(wands.sales==0,"Cancelled interaction must not sell");
                        event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,new Stack("wand",1),block,BlockFace.UP,EquipmentSlot.HAND);
                        enabled.add("Bolt"); enabled.add("SuperiorSkyblock2");
                        NextGens.bolt=proxy(org.popcraft.bolt.BoltAPI.class,(p,m,a)->true);
                        var ownIsland=proxy(com.bgsoftware.superiorskyblock.api.island.Island.class,(p,m,a)->OWNER);
                        com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.player=proxy(com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer.class,(p,m,a)->ownIsland);
                        com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.island=proxy(com.bgsoftware.superiorskyblock.api.island.Island.class,(p,m,a)->UUID.fromString("00000000-0000-0000-0000-000000000002"));
                        listener.onInteract(event);
                        check(wands.sales==0,"Bolt allowing an unlocked chest must not bypass the island denial");
                        enabled.clear(); event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,new Stack("wand",1),block,BlockFace.UP,EquipmentSlot.HAND);
                        listener.onInteract(event);
                        check(wands.sales==1,"Allowed container interaction must still sell");
                        System.out.println("Selling checks passed: "+checks);
                    }
                }
                """);
        List<String> compilerArgs = new ArrayList<>(List.of("-classpath", args[0], "-d", output.toString()));
        for (var entry : sources.entrySet()) {
            Path source = output.resolve(entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, entry.getValue());
            compilerArgs.add(source.toString());
        }
        for (String source : List.of(
                "sell/SellManager.java", "sell/SellDataCalculator.java", "utils/SellData.java", "users/models/User.java",
                "api/events/sell/SellEvent.java", "api/events/sell/SellCommandUseEvent.java", "api/events/sell/SellwandUseEvent.java",
                "sellwand/models/SellwandData.java", "gui/ShopInventory.java", "sellwand/listeners/SellwandListener.java")) {
            compilerArgs.add("src/main/java/com/muhammaddaffa/nextgens/" + source);
        }
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, compilerArgs.toArray(String[]::new));
        if (result != 0) throw new AssertionError("Selling check compilation failed");
        List<URL> urls = new ArrayList<>(List.of(output.toUri().toURL()));
        for (String path : args[0].split(java.io.File.pathSeparator)) urls.add(Path.of(path).toUri().toURL());
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("SellingCases").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }
}
