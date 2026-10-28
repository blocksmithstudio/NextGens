import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Run from the project root: java src/test/checks/MenuCheck.java "<Maven test classpath>". */
public class MenuCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the Maven dependency classpath");
        Path output = Files.createTempDirectory(Files.createDirectories(Path.of("target")), "menu-check-");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("com.muhammaddaffa.mdlib.utils.Config", """
                package com.muhammaddaffa.mdlib.utils;
                public class Config {
                    public final org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
                    public org.bukkit.configuration.file.FileConfiguration getConfig() { return config; }
                    public int getInt(String key) { return config.getInt(key); }
                    public int getInt(String key, int fallback) { return config.getInt(key, fallback); }
                    public String getString(String key) { return config.getString(key); }
                    public boolean getBoolean(String key) { return config.getBoolean(key); }
                    public java.util.List<Integer> getIntegerList(String key) { return config.getIntegerList(key); }
                    public java.util.List<String> getStringList(String key) { return config.getStringList(key); }
                    public void sendMessage(org.bukkit.command.CommandSender player, String key, Placeholder... placeholders) { }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.Common", """
                package com.muhammaddaffa.mdlib.utils;
                public class Common {
                    public static String color(String value) { return value; }
                    public static String digits(double value) { return Double.toString(value); }
                    public static String format(double value) { return Double.toString(value); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.Placeholder", """
                package com.muhammaddaffa.mdlib.utils;
                public class Placeholder { public Placeholder add(String key, Object value) { return this; } }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.ItemBuilder", """
                package com.muhammaddaffa.mdlib.utils;
                public class ItemBuilder {
                    public static org.bukkit.entity.Player lastPlayer;
                    private final org.bukkit.inventory.ItemStack stack;
                    public ItemBuilder(org.bukkit.inventory.ItemStack stack) { this.stack=stack; }
                    public static ItemBuilder fromConfig(org.bukkit.entity.Player player, Object config, String key) { lastPlayer = player; return new ItemBuilder(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE)); }
                    public ItemBuilder addLore(java.util.List<String> lore) { return this; }
                    public ItemBuilder lore(java.util.List<String> lore) { return this; }
                    public ItemBuilder placeholder(Placeholder value) { return this; }
                    public ItemBuilder flags(org.bukkit.inventory.ItemFlag... flags) { return this; }
                    public ItemBuilder amount(int value) { stack.setAmount(value); return this; }
                    public org.bukkit.inventory.ItemStack build() { return stack; }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.xseries.XSound", """
                package com.muhammaddaffa.mdlib.xseries;
                public enum XSound { UI_BUTTON_CLICK; public org.bukkit.Sound get() { return null; } }
                """);
        sources.put("com.muhammaddaffa.mdlib.fastinv.FastInv", """
                package com.muhammaddaffa.mdlib.fastinv;
                public class FastInv {
                    public final java.util.Map<Integer,java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent>> handlers = new java.util.HashMap<>();
                    public final java.util.Map<Integer,org.bukkit.inventory.ItemStack> rendered = new java.util.HashMap<>();
                    public FastInv(int size, String title) { }
                    public void open(org.bukkit.entity.Player player) { }
                    public void clearItems() { handlers.clear(); rendered.clear(); }
                    public void addCloseHandler(java.util.function.Consumer<org.bukkit.event.inventory.InventoryCloseEvent> handler) { }
                    public void setItem(int slot, org.bukkit.inventory.ItemStack stack) { rendered.put(slot,stack); }
                    public void setItem(int slot, org.bukkit.inventory.ItemStack stack, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) { setItem(slot,stack); handlers.put(slot,handler); }
                    public void setItems(int[] slots, org.bukkit.inventory.ItemStack stack, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) { for(int slot:slots) setItem(slot,stack,handler); }
                    public void setItems(java.util.List<Integer> slots, org.bukkit.inventory.ItemStack stack, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) { for(int slot:slots) setItem(slot,stack,handler); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.Generator", """
                package com.muhammaddaffa.nextgens.generators;
                public record Generator(String id, org.bukkit.inventory.ItemStack item) {
                    public String displayName() { return id; }
                    public String nextTier() { return id; }
                    public double cost() { return 1; }
                    public org.bukkit.inventory.ItemStack createItem(int amount) { return item.clone(); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.managers.GeneratorManager", """
                package com.muhammaddaffa.nextgens.generators.managers;
                import com.muhammaddaffa.nextgens.generators.*;
                public class GeneratorManager {
                    public final java.util.List<ActiveGenerator> active = new java.util.ArrayList<>();
                    public java.util.List<ActiveGenerator> getActiveGenerator(java.util.UUID owner) { return active.stream().filter(a->a.getOwner().equals(owner)).toList(); }
                    public java.util.List<ActiveGenerator> getActiveGenerator(org.bukkit.entity.Player player) { return getActiveGenerator(player.getUniqueId()); }
                    public synchronized boolean unregisterGenerator(ActiveGenerator expected) { return active.remove(expected); }
                    public int getGeneratorCount(org.bukkit.entity.Player player) { return getActiveGenerator(player).size(); }
                    public Generator getGenerator(String id) { return active.stream().map(ActiveGenerator::getGenerator).filter(g->g.id().equals(id)).findFirst().orElse(null); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.users.models.User", """
                package com.muhammaddaffa.nextgens.users.models;
                public record User(java.util.UUID id) { public java.util.UUID getUniqueId() { return id; } public String getName() { return "Fixture"; } }
                """);
        sources.put("com.muhammaddaffa.nextgens.users.UserManager", """
                package com.muhammaddaffa.nextgens.users;
                public class UserManager { public int getMaxSlot(org.bukkit.entity.Player player) { return 999; } }
                """);
        sources.put("com.muhammaddaffa.nextgens.refund.RefundManager", """
                package com.muhammaddaffa.nextgens.refund;
                public class RefundManager {
                    public int delivered;
                    public void giveGeneratorItems(org.bukkit.entity.Player player, com.muhammaddaffa.nextgens.generators.Generator generator, int amount) { delivered+=amount; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorUpdateHelper", """
                package com.muhammaddaffa.nextgens.generators.listeners.helpers;
                public class GeneratorUpdateHelper {
                    public static boolean upgradeGenerator(org.bukkit.entity.Player player, com.muhammaddaffa.nextgens.generators.ActiveGenerator active, boolean silent) { return false; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.Utils", """
                package com.muhammaddaffa.nextgens.utils;
                public class Utils { public static int[] convertListToIntArray(java.util.List<Integer> slots) { return slots.stream().mapToInt(Integer::intValue).toArray(); } }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.VisualAction", """
                package com.muhammaddaffa.nextgens.utils;
                public class VisualAction { public static void send(org.bukkit.entity.Player player, org.bukkit.configuration.file.FileConfiguration config, String key, com.muhammaddaffa.mdlib.utils.Placeholder values) { } }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.GensRunnable", """
                package com.muhammaddaffa.nextgens.utils;
                public abstract class GensRunnable implements Runnable {
                    public void cancel() { }
                    public void runTaskTimer(Object plugin, long delay, long interval) { }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.NextGens", """
                package com.muhammaddaffa.nextgens;
                public class NextGens {
                    public static final com.muhammaddaffa.mdlib.utils.Config DEFAULT_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.mdlib.utils.Config VIEW_GUI_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.mdlib.utils.Config UPGRADE_GENS_GUI_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.nextgens.generators.managers.GeneratorManager generators = new com.muhammaddaffa.nextgens.generators.managers.GeneratorManager();
                    public static final com.muhammaddaffa.nextgens.refund.RefundManager refunds = new com.muhammaddaffa.nextgens.refund.RefundManager();
                    private static final NextGens instance = new NextGens();
                    public static NextGens getInstance() { return instance; }
                    public com.muhammaddaffa.nextgens.generators.managers.GeneratorManager getGeneratorManager() { return generators; }
                    public com.muhammaddaffa.nextgens.refund.RefundManager getRefundManager() { return refunds; }
                }
                """);
        sources.put("MenuCases", """
                import com.muhammaddaffa.nextgens.NextGens;
                import com.muhammaddaffa.nextgens.gui.*;
                import com.muhammaddaffa.nextgens.gui.helpers.Pagination;
                import com.muhammaddaffa.nextgens.generators.*;
                import com.muhammaddaffa.nextgens.users.*;
                import com.muhammaddaffa.nextgens.users.models.User;
                import com.muhammaddaffa.mdlib.fastinv.FastInv;
                import org.bukkit.*;
                import org.bukkit.block.Block;
                import org.bukkit.entity.Player;
                import org.bukkit.inventory.ItemStack;
                import org.bukkit.configuration.file.FileConfiguration;
                import java.lang.reflect.*;
                import java.util.*;
                import java.util.logging.Logger;
                public class MenuCases {
                    static final UUID OWNER=UUID.fromString("00000000-0000-0000-0000-000000000001");
                    static final Set<String> permissions=new HashSet<>();
                    static int checks, cleared;
                    static World world;
                    static Player player;
                    static <T> T proxy(Class<T> type, InvocationHandler handler) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler)); }
                    static void check(boolean result,String message) { if(!result) throw new AssertionError(message); checks++; }
                    static class Stack extends ItemStack {
                        final String id; int amount=1;
                        Stack(String id) { this.id=id; }
                        @Override public Stack clone() { return new Stack(id); }
                        @Override public void setAmount(int value) { amount=value; }
                        @Override public int getAmount() { return amount; }
                    }
                    static Pagination<?> page(Object menu) throws Exception { Field field=menu.getClass().getDeclaredField("pagination"); field.setAccessible(true); return (Pagination<?>)field.get(menu); }
                    static void click(FastInv menu,int slot) { var handler=menu.handlers.get(slot); check(handler!=null,"Button must be present in slot "+slot); handler.accept(null); }
                    static void configure(FileConfiguration config,String slots) {
                        config.set("size",9);config.set("title","Fixture {player}");config.set(slots,List.of(0,1));
                        config.set("items.next.type","next_page");config.set("items.next.slots",List.of(7));
                        config.set("items.previous.type","previous_page");config.set("items.previous.slots",List.of(8));
                    }
                    static ActiveGenerator active(String id,int x) { return new ActiveGenerator(OWNER,new Location(world,x,64,0),new Generator(id,new Stack(id))); }
                    static void seed() { NextGens.generators.active.clear();NextGens.generators.active.addAll(List.of(active("a",0),active("b",1),active("c",2))); }
                    public static void main(String[] args) throws Exception {
                        Block block=proxy(Block.class,(p,m,a)-> { if(m.getName().equals("setType")) cleared++;return null; });
                        world=proxy(World.class,(p,m,a)->switch(m.getName()) {case "getName"->"fixture";case "getBlockAt"->block;default->null;});
                        player=proxy(Player.class,(p,m,a)->switch(m.getName()) {case "getUniqueId"->OWNER;case "getName"->"Fixture";case "hasPermission"->permissions.contains(a[0]);case "getLocation"->new Location(world,0,64,0);default->null;});
                        Bukkit.setServer(proxy(Server.class,(p,m,a)->switch(m.getName()) {case "getLogger"->Logger.getLogger("menu-check");case "getName","getVersion","getBukkitVersion"->"fixture";default->null;}));
                        var config=NextGens.VIEW_GUI_CONFIG.config; configure(config,"slots");
                        config.set("pickup-own","fixture.pickup.own");config.set("pickup-others","fixture.pickup.others");
                        permissions.add("fixture.pickup.own");seed();
                        var menu=new ViewInventory(player,new User(OWNER),NextGens.generators,new UserManager());
                        check(com.muhammaddaffa.mdlib.utils.ItemBuilder.lastPlayer==player,"View items pass the viewing player to PlaceholderAPI");
                        check(page(menu).currentPage==1 && menu.rendered.containsKey(1),"First view page has full contents");
                        click(menu,8);check(page(menu).currentPage==1,"Previous on first view page cannot underflow");
                        click(menu,7);check(page(menu).currentPage==2 && ((Stack)menu.rendered.get(0)).id.equals("c") && !menu.rendered.containsKey(1),"View navigation reaches final singleton page");
                        click(menu,7);check(page(menu).currentPage==2,"Next on final view page does not overflow");
                        click(menu,8);check(page(menu).currentPage==1,"Previous works from final view page");
                        click(menu,7);var stale=menu.handlers.get(0);
                        permissions.clear();stale.accept(null);
                        check(NextGens.generators.active.size()==3 && NextGens.refunds.delivered==0 && cleared==0,"Pickup rechecks permission after GUI opens");
                        permissions.add("fixture.pickup.own");stale.accept(null);
                        check(NextGens.generators.active.size()==2 && NextGens.refunds.delivered==1 && cleared==1,"Authorized pickup unregisters, clears and refunds exactly once");
                        check(page(menu).currentPage==1 && page(menu).totalPage==1,"Removing last item clamps refreshed page to the new last page");
                        stale.accept(null);
                        check(NextGens.refunds.delivered==1 && cleared==1,"Repeated stale pickup callback cannot clear or refund twice");
                        seed();menu=new ViewInventory(player,new User(OWNER),NextGens.generators,new UserManager());
                        var replacedHandler=menu.handlers.get(0);NextGens.generators.active.set(0,active("replacement",0));replacedHandler.accept(null);
                        check(NextGens.refunds.delivered==1 && cleared==1 && NextGens.generators.active.get(0).getGenerator().id().equals("replacement"),"Stale GUI reference cannot remove a replacement generator");
                        permissions.clear();permissions.add("fixture.pickup.others");
                        Player visitor=proxy(Player.class,(p,m,a)->switch(m.getName()) {case "getUniqueId"->UUID.fromString("00000000-0000-0000-0000-000000000002");case "hasPermission"->permissions.contains(a[0]);default->null;});
                        var others=new ViewInventory(visitor,new User(OWNER),NextGens.generators,new UserManager());
                        permissions.clear();others.handlers.get(0).accept(null);
                        check(NextGens.refunds.delivered==1 && cleared==1,"Other-owner pickup also rechecks current permission");
                        configure(NextGens.UPGRADE_GENS_GUI_CONFIG.config,"item-slots");seed();
                        var upgrade=new UpgradeGensInventory(player);
                        check(com.muhammaddaffa.mdlib.utils.ItemBuilder.lastPlayer==player,"Upgrade items pass the viewing player to PlaceholderAPI");
                        click(upgrade,7);check(page(upgrade).currentPage==2 && ((Stack)upgrade.rendered.get(0)).id.equals("c") && !upgrade.rendered.containsKey(1),"Upgrade navigation reaches final singleton page");
                        upgrade.setAllItems();check(page(upgrade).currentPage==2,"Upgrade refresh preserves selected page");
                        click(upgrade,7);check(page(upgrade).currentPage==2,"Upgrade final page cannot overflow");
                        click(upgrade,8);check(page(upgrade).currentPage==1,"Upgrade previous navigation works");
                        click(upgrade,8);check(page(upgrade).currentPage==1,"Upgrade first page cannot underflow");
                        click(upgrade,7);NextGens.generators.active.removeLast();upgrade.setAllItems();
                        check(page(upgrade).currentPage==1 && page(upgrade).totalPage==1,"Upgrade refresh clamps after page count shrinks");
                        NextGens.generators.active.clear();upgrade.setAllItems();
                        check(page(upgrade).currentPage==1 && page(upgrade).getItems().isEmpty(),"Empty upgrade refresh retains a valid empty page");
                        System.out.println("PASS: "+checks+" menu checks against actual GUI and pagination sources");
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
        for (String source : List.of("gui/ViewInventory.java", "gui/UpgradeGensInventory.java", "gui/helpers/Pagination.java", "generators/ActiveGenerator.java")) {
            compilerArgs.add("src/main/java/com/muhammaddaffa/nextgens/" + source);
        }
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, compilerArgs.toArray(String[]::new));
        if (result != 0) throw new AssertionError("Menu check compilation failed");
        List<URL> urls = new ArrayList<>(List.of(output.toUri().toURL()));
        for (String path : args[0].split(java.io.File.pathSeparator)) urls.add(Path.of(path).toUri().toURL());
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("MenuCases").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }
}
