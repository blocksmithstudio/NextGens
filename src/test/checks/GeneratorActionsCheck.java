import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Run from the project root: java src/test/checks/GeneratorActionsCheck.java "<Maven test classpath>". */
public class GeneratorActionsCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the Maven dependency classpath");
        Path output = Files.createTempDirectory(Files.createDirectories(Path.of("target")), "generator-actions-check-");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("com.muhammaddaffa.mdlib.utils.Config", """
                package com.muhammaddaffa.mdlib.utils;
                public class Config {
                    public final org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
                    public org.bukkit.configuration.file.FileConfiguration getConfig() { return config; }
                    public int getInt(String key) { return config.getInt(key); }
                    public String getString(String key) { return config.getString(key); }
                    public java.util.List<Integer> getIntegerList(String key) { return config.getIntegerList(key); }
                    public void sendMessage(org.bukkit.command.CommandSender player, String key, Placeholder... placeholders) { }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.ItemBuilder", """
                package com.muhammaddaffa.mdlib.utils;
                public class ItemBuilder {
                    public static org.bukkit.entity.Player lastPlayer;
                    public ItemBuilder(org.bukkit.Material material) { }
                    public static ItemBuilder fromConfig(org.bukkit.entity.Player player, org.bukkit.configuration.file.FileConfiguration config, String key) { lastPlayer = player; return new ItemBuilder(org.bukkit.Material.STONE); }
                    public ItemBuilder name(String name) { return this; }
                    public ItemBuilder customModelData(int data) { return this; }
                    public ItemBuilder lore(java.util.List<String> lore) { return this; }
                    public ItemBuilder flags(org.bukkit.inventory.ItemFlag... flags) { return this; }
                    public ItemBuilder placeholder(Placeholder placeholder) { return this; }
                    public ItemBuilder enchant(org.bukkit.enchantments.Enchantment enchantment) { return this; }
                    public org.bukkit.inventory.ItemStack build() { return new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.fastinv.FastInv", """
                package com.muhammaddaffa.mdlib.fastinv;
                public class FastInv {
                    public static final java.util.List<java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent>> handlers = new java.util.ArrayList<>();
                    public FastInv(int size, String title) { handlers.clear(); }
                    public void setItems(int[] slots, org.bukkit.inventory.ItemStack stack) { }
                    public void setItems(int[] slots, org.bukkit.inventory.ItemStack stack, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) { handlers.add(handler); }
                    public void setItems(java.util.List<Integer> slots, org.bukkit.inventory.ItemStack stack, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) { handlers.add(handler); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.hooks.VaultEconomy", """
                package com.muhammaddaffa.mdlib.hooks;
                import net.milkbowl.vault.economy.EconomyResponse;
                public class VaultEconomy {
                    public static int mode, withdrawals, refunds;
                    public static double balance = 1000, charged, refunded;
                    public static Runnable callback;
                    public static double getBalance(org.bukkit.OfflinePlayer player) { return balance; }
                    public static EconomyResponse withdraw(org.bukkit.OfflinePlayer player, double value) {
                        withdrawals++;
                        if (callback != null) { Runnable action=callback; callback=null; action.run(); }
                        if (mode == 2) throw new IllegalStateException("Simulated unavailable economy");
                        if (mode == 3) return null;
                        if (mode == 0) charged+=value;
                        return new EconomyResponse(value, balance, mode == 1 ? EconomyResponse.ResponseType.FAILURE : EconomyResponse.ResponseType.SUCCESS, "");
                    }
                    public static EconomyResponse deposit(org.bukkit.OfflinePlayer player, double value) {
                        refunds++; refunded+=value;
                        return new EconomyResponse(value, balance, EconomyResponse.ResponseType.SUCCESS, "");
                    }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.task.ExecutorManager", """
                package com.muhammaddaffa.mdlib.task;
                public class ExecutorManager {
                    private static final ExecutorProvider provider = (ExecutorProvider) java.lang.reflect.Proxy.newProxyInstance(ExecutorProvider.class.getClassLoader(), new Class[]{ExecutorProvider.class}, (p,m,a) -> null);
                    public static ExecutorProvider getProvider() { return provider; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.users.UserManager", """
                package com.muhammaddaffa.nextgens.users;
                public class UserManager {
                    public final com.muhammaddaffa.nextgens.users.models.User user = new com.muhammaddaffa.nextgens.users.models.User(java.util.UUID.randomUUID());
                    public com.muhammaddaffa.nextgens.users.models.User getUser(java.util.UUID player) { return user; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.managers.GeneratorManager", """
                package com.muhammaddaffa.nextgens.generators.managers;
                import com.muhammaddaffa.nextgens.generators.*;
                public class GeneratorManager {
                    public final java.util.List<ActiveGenerator> active = new java.util.ArrayList<>();
                    public Generator next;
                    public int registrations, saves;
                    public ActiveGenerator getActiveGenerator(org.bukkit.Location location) { return active.stream().filter(g -> g.getLocation().equals(location)).findFirst().orElse(null); }
                    public java.util.List<ActiveGenerator> getActiveGenerator(org.bukkit.entity.Player player) { return active.stream().filter(g -> g.getOwner().equals(player.getUniqueId())).toList(); }
                    public Generator getGenerator(String id) { return next; }
                    public synchronized ActiveGenerator registerGenerator(java.util.UUID owner, Generator generator, org.bukkit.block.Block block) {
                        registrations++;
                        ActiveGenerator found = getActiveGenerator(block.getLocation());
                        if(found == null) { found=new ActiveGenerator(owner,block.getLocation(),generator); active.add(found); }
                        else found.setGenerator(generator);
                        return found;
                    }
                    public void saveActiveGenerator(ActiveGenerator generator) { saves++; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.Utils", """
                package com.muhammaddaffa.nextgens.utils;
                public class Utils {
                    public static int cashbacks;
                    public static void bassSound(org.bukkit.entity.Player player) { }
                    public static void performCashback(org.bukkit.entity.Player player, com.muhammaddaffa.nextgens.users.UserManager users, double cost) { cashbacks++; }
                    public static int[] convertListToIntArray(java.util.List<Integer> list) { return list.stream().mapToInt(Integer::intValue).toArray(); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.VisualAction", """
                package com.muhammaddaffa.nextgens.utils;
                public class VisualAction {
                    public static void send(org.bukkit.entity.Player player, org.bukkit.configuration.file.FileConfiguration config, String key, com.muhammaddaffa.mdlib.utils.Placeholder placeholder) { }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorParticle", """
                package com.muhammaddaffa.nextgens.generators.listeners.helpers;
                public class GeneratorParticle { public static void successParticle(org.bukkit.block.Block block, com.muhammaddaffa.nextgens.generators.Generator generator) { } }
                """);
        sources.put("com.muhammaddaffa.nextgens.utils.Settings", """
                package com.muhammaddaffa.nextgens.utils;
                public class Settings {
                    public static int CORRUPT_GUI_SIZE=9;
                    public static String CORRUPT_GUI_TITLE="Repair";
                    public static java.util.List<Integer> CORRUPT_GUI_DISPLAY_SLOTS=java.util.List.of(2);
                }
                """);
        sources.put("me.clip.placeholderapi.PlaceholderAPI", """
                package me.clip.placeholderapi;
                public class PlaceholderAPI {
                    public static String setPlaceholders(org.bukkit.OfflinePlayer player, String text) { return text; }
                    public static java.util.List<String> setPlaceholders(org.bukkit.OfflinePlayer player, java.util.List<String> text) { return text; }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.NextGens", """
                package com.muhammaddaffa.nextgens;
                public class NextGens {
                    public static boolean STOPPING;
                    public static final com.muhammaddaffa.mdlib.utils.Config DEFAULT_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.mdlib.utils.Config UPGRADE_GUI_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.mdlib.utils.Config CORRUPT_GUI_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static final com.muhammaddaffa.nextgens.generators.managers.GeneratorManager manager = new com.muhammaddaffa.nextgens.generators.managers.GeneratorManager();
                    public static final com.muhammaddaffa.nextgens.users.UserManager users = new com.muhammaddaffa.nextgens.users.UserManager();
                    private static final NextGens instance = new NextGens();
                    public static NextGens getInstance() { return instance; }
                    public com.muhammaddaffa.nextgens.users.UserManager getUserManager() { return users; }
                    public com.muhammaddaffa.nextgens.generators.managers.GeneratorManager getGeneratorManager() { return manager; }
                    public java.util.logging.Logger getLogger() { return java.util.logging.Logger.getLogger("generator-check"); }
                }
                """);
        sources.put("GeneratorActionCases", CASES);
        List<String> compilerArgs = new ArrayList<>(List.of("-classpath", args[0], "-d", output.toString()));
        for (var entry : sources.entrySet()) {
            Path source = output.resolve(entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, entry.getValue());
            compilerArgs.add(source.toString());
        }
        for (String source : List.of("generators/ActiveGenerator.java", "users/models/User.java",
                "api/events/generators/GeneratorEvent.java", "api/events/generators/GeneratorUpgradeEvent.java",
                "generators/listeners/helpers/GeneratorActionHelper.java", "generators/listeners/helpers/GeneratorUpdateHelper.java",
                "generators/listeners/helpers/GeneratorFixHelper.java", "gui/UpgradeInventory.java", "gui/FixInventory.java")) {
            compilerArgs.add("src/main/java/com/muhammaddaffa/nextgens/" + source);
        }
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, compilerArgs.toArray(String[]::new));
        if (result != 0) throw new AssertionError("Generator action check compilation failed");
        List<URL> urls = new ArrayList<>(List.of(output.toUri().toURL()));
        for (String path : args[0].split(java.io.File.pathSeparator)) urls.add(Path.of(path).toUri().toURL());
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("GeneratorActionCases").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }

    private static final String CASES = """
            import com.muhammaddaffa.nextgens.NextGens;
            import com.muhammaddaffa.nextgens.generators.*;
            import com.muhammaddaffa.nextgens.generators.listeners.helpers.*;
            import com.muhammaddaffa.nextgens.api.events.generators.GeneratorUpgradeEvent;
            import com.muhammaddaffa.nextgens.gui.*;
            import com.muhammaddaffa.nextgens.utils.Utils;
            import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
            import com.muhammaddaffa.mdlib.fastinv.FastInv;
            import org.bukkit.*;
            import org.bukkit.block.Block;
            import org.bukkit.entity.Player;
            import org.bukkit.inventory.ItemStack;
            import org.bukkit.plugin.PluginManager;
            import java.lang.reflect.*;
            import java.util.*;
            import java.util.function.Consumer;
            import java.util.logging.Logger;

            public class GeneratorActionCases {
                static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
                static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000002");
                static Consumer<GeneratorUpgradeEvent> eventHandler=event->{};
                static int checks;
                static boolean permission=true;
                static <T> T proxy(Class<T> type, InvocationHandler handler) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler)); }
                static void check(boolean value,String message) { if(!value) throw new AssertionError(message); checks++; }
                static Generator tier(double cost,double repair) { return new Generator("test","Test",10,new ItemStack(Material.STONE),List.of(),"next",cost,true,repair,0,false,List.of(),List.of()); }
                static Location location;
                static Player owner,member;
                static ActiveGenerator reset(double cost,double repair,boolean corrupted) {
                    eventHandler=event->{}; VaultEconomy.mode=0; VaultEconomy.withdrawals=0; VaultEconomy.refunds=0;
                    VaultEconomy.charged=0; VaultEconomy.refunded=0; VaultEconomy.balance=1000; VaultEconomy.callback=null;
                    Utils.cashbacks=0; permission=true; NextGens.STOPPING=false;
                    NextGens.users.user.clearMember(); NextGens.DEFAULT_CONFIG.config.set("repair-owner-only",true);
                    NextGens.manager.active.clear(); NextGens.manager.saves=0; NextGens.manager.registrations=0; NextGens.manager.next=tier(50,5);
                    ActiveGenerator active=new ActiveGenerator(OWNER,location,tier(cost,repair)); active.setCorrupted(corrupted);
                    NextGens.manager.active.add(active); return active;
                }
                static boolean upgrade(Player player,ActiveGenerator active) { return GeneratorUpdateHelper.upgradeGenerator(player,active,active.getGenerator(),NextGens.manager.next,true); }
                static void fix(ActiveGenerator active) { GeneratorFixHelper.fixGenerator(owner,active,active.getGenerator()); }
                static void unchanged(ActiveGenerator active,boolean corrupted,String label) { check(active.isCorrupted()==corrupted && NextGens.manager.registrations==0 && NextGens.manager.saves==0 && Utils.cashbacks==0,label); }
                public static void main(String[] args) throws Exception {
                    PluginManager plugins=proxy(PluginManager.class,(p,m,a)-> {if(m.getName().equals("callEvent")) eventHandler.accept((GeneratorUpgradeEvent)a[0]); return null;});
                    Bukkit.setServer(proxy(Server.class,(p,m,a)->switch(m.getName()) {
                        case "getPluginManager"->plugins;
                        case "getLogger"->Logger.getLogger("generator-check");
                        case "getName","getVersion","getBukkitVersion"->"generator-check";
                        default->null;
                    }));
                    World world=proxy(World.class,(p,m,a)->switch(m.getName()) {
                        case "getUID"->OWNER; case "getName"->"TestWorld";
                        case "hashCode"->System.identityHashCode(p); case "equals"->p==a[0];
                        case "getBlockAt"->proxy(Block.class,(b,bm,ba)->bm.getName().equals("getLocation")?location:null);
                        default->null;
                    });
                    location=new Location(world,1,64,1);
                    owner=proxy(Player.class,(p,m,a)->switch(m.getName()) {case "getUniqueId"->OWNER;case "hasPermission"->permission;case "getName"->"Owner";default->null;});
                    member=proxy(Player.class,(p,m,a)->switch(m.getName()) {case "getUniqueId"->MEMBER;case "hasPermission"->permission;case "getName"->"Member";default->null;});
                    for(int mode=1;mode<=3;mode++) {
                        ActiveGenerator active=reset(25,5,false); VaultEconomy.mode=mode;
                        check(!upgrade(owner,active),"Rejected withdrawal must reject upgrade"); unchanged(active,false,"Rejected upgrade mutated generator");
                        active=reset(25,5,true); VaultEconomy.mode=mode; fix(active); unchanged(active,true,"Rejected repair mutated generator");
                        active=reset(25,5,true); VaultEconomy.mode=mode; GeneratorFixHelper.fixGenerators(owner,NextGens.manager); unchanged(active,true,"Rejected bulk repair mutated generator");
                    }
                    for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
                        ActiveGenerator active=reset(invalid,5,false); check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Invalid upgrade price reached economy");
                        active=reset(25,invalid,true); fix(active); unchanged(active,true,"Invalid repair price mutated generator"); check(VaultEconomy.withdrawals==0,"Invalid repair price reached economy");
                        GeneratorFixHelper.fixGenerators(owner,NextGens.manager); check(VaultEconomy.withdrawals==0 && active.isCorrupted(),"Invalid bulk repair price reached economy");
                    }
                    ActiveGenerator active=reset(25,5,false); VaultEconomy.balance=0; check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Insufficient funds must not charge");
                    active=reset(25,5,false); VaultEconomy.balance=Double.NaN; check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Non-finite balance must not charge");
                    active=reset(25,Double.MAX_VALUE,true); NextGens.manager.active.add(new ActiveGenerator(OWNER,new Location(world,2,64,1),tier(25,Double.MAX_VALUE),0,true)); GeneratorFixHelper.fixGenerators(owner,NextGens.manager); check(active.isCorrupted() && VaultEconomy.withdrawals==0,"Overflowing bulk total must not charge or repair");
                    active=reset(0,0,false); check(upgrade(owner,active) && VaultEconomy.withdrawals==0,"Free upgrade must work without provider zero withdrawal");
                    active=reset(25,0,true); fix(active); check(!active.isCorrupted() && NextGens.manager.saves==1 && VaultEconomy.withdrawals==0,"Free repair must be saved");
                    active=reset(25,5,false); eventHandler=e->e.setCancelled(true); check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Cancelled event must not charge");
                    active=reset(25,5,false); NextGens.manager.active.clear(); check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Picked up generator must not be recreated");
                    active=reset(25,5,false); NextGens.manager.active.set(0,new ActiveGenerator(OWNER,location,active.getGenerator())); check(!upgrade(owner,active),"Replacement instance must not be overwritten");
                    active=reset(25,5,false); Generator captured=active.getGenerator(); active.setGenerator(tier(30,6)); check(!GeneratorUpdateHelper.upgradeGenerator(owner,active,captured,NextGens.manager.next,true),"Stale tier must not be overwritten");
                    active=reset(25,5,true); check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Corrupted generator cannot upgrade");
                    active=reset(25,5,false); final ActiveGenerator picked=active; eventHandler=e->NextGens.manager.active.clear(); check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Event pickup must abort upgrade");
                    active=reset(25,5,false); final ActiveGenerator corrupted=active; eventHandler=e->corrupted.setCorrupted(true); check(!upgrade(owner,active) && VaultEconomy.withdrawals==0,"Event corruption must abort upgrade");
                    active=reset(25,5,false); NextGens.users.user.addMember(MEMBER); eventHandler=e->NextGens.users.user.removeMember(MEMBER); check(!upgrade(member,active) && VaultEconomy.withdrawals==0,"Event revoked trust must abort upgrade");
                    active=reset(25,5,false); final ActiveGenerator recursive=active; eventHandler=e->check(!upgrade(owner,recursive),"Recursive event upgrade must be rejected"); check(upgrade(owner,active) && VaultEconomy.withdrawals==1 && NextGens.manager.registrations==1,"Upgrade must run once");
                    active=reset(25,5,false); NextGens.users.user.addMember(MEMBER); check(upgrade(member,active) && active.getOwner().equals(OWNER),"Trusted upgrade must preserve owner");
                    active=reset(25,5,false); VaultEconomy.callback=()->NextGens.manager.active.clear(); check(!upgrade(owner,active) && VaultEconomy.refunds==1 && VaultEconomy.refunded==25 && NextGens.manager.registrations==0,"Provider pickup must refund without recreation");
                    active=reset(25,5,false); final ActiveGenerator changed=active; Generator changedTier=tier(60,10); VaultEconomy.callback=()->changed.setGenerator(changedTier); check(!upgrade(owner,active) && VaultEconomy.refunds==1 && active.getGenerator()==changedTier,"Provider tier change must be preserved and refunded");
                    active=reset(25,5,true); NextGens.manager.active.clear(); fix(active); unchanged(active,true,"Picked up repair must not mutate stale instance");
                    active=reset(25,5,true); Generator staleRepair=active.getGenerator(); active.setGenerator(tier(30,6)); GeneratorFixHelper.fixGenerator(owner,active,staleRepair); check(VaultEconomy.withdrawals==0 && active.isCorrupted(),"Stale repair tier must not charge");
                    active=reset(25,5,true); NextGens.manager.active.set(0,new ActiveGenerator(OWNER,location,active.getGenerator(),0,true)); fix(active); check(VaultEconomy.withdrawals==0 && active.isCorrupted(),"Replacement repair instance must not charge");
                    active=reset(25,5,false); fix(active); check(VaultEconomy.withdrawals==0,"Already repaired generator must not be charged");
                    active=reset(25,5,true); GeneratorFixHelper.fixGenerator(member,active,active.getGenerator()); check(VaultEconomy.withdrawals==0 && active.isCorrupted(),"Repair must enforce current access");
                    NextGens.DEFAULT_CONFIG.config.set("repair-owner-only",false); GeneratorFixHelper.fixGenerator(member,active,active.getGenerator()); check(!active.isCorrupted() && NextGens.manager.saves==1,"Public repairs must remain supported");
                    active=reset(25,5,true); VaultEconomy.callback=()->NextGens.manager.active.clear(); fix(active); unchanged(active,true,"Provider pickup must not mutate stale repair"); check(VaultEconomy.refunded==5,"Provider pickup repair must refund");
                    active=reset(25,5,true); permission=false; GeneratorFixHelper.fixGenerators(owner,NextGens.manager); check(VaultEconomy.withdrawals==0 && active.isCorrupted(),"Bulk repair must enforce permission");
                    active=reset(25,5,true); VaultEconomy.callback=()->permission=false; GeneratorFixHelper.fixGenerators(owner,NextGens.manager); check(VaultEconomy.refunded==5 && active.isCorrupted(),"Bulk permission revoked in provider must refund");
                    active=reset(25,5,true); GeneratorFixHelper.fixGenerators(owner,NextGens.manager); check(!active.isCorrupted() && NextGens.manager.saves==1 && Utils.cashbacks==1,"Bulk repair must persist and cashback once");
                    var config=NextGens.UPGRADE_GUI_CONFIG.config;
                    config.set("size",9); config.set("title","Upgrade"); config.set("confirm-slots",List.of(0)); config.set("cancel-slots",List.of(1)); config.set("display-slots",List.of(2));
                    active=reset(25,5,false); new UpgradeInventory(owner,active,active.getGenerator(),NextGens.manager.next,NextGens.manager,NextGens.users);
                    NextGens.manager.active.clear(); FastInv.handlers.getFirst().accept(null); check(VaultEconomy.withdrawals==0 && NextGens.manager.registrations==0,"Upgrade confirm must reject stale menu");
                    active=reset(25,5,true); new FixInventory(owner,active,active.getGenerator(),NextGens.users,NextGens.manager);
                    FastInv.handlers.getFirst().accept(null); check(!active.isCorrupted() && NextGens.manager.saves==1 && Utils.cashbacks==1,"Repair confirm must use persisted cashback path");
                    FastInv.handlers.getFirst().accept(null); check(VaultEconomy.withdrawals==1 && NextGens.manager.saves==1 && Utils.cashbacks==1,"Repeated repair click must not double charge");
                    active=reset(25,5,true); VaultEconomy.mode=1; new FixInventory(owner,active,active.getGenerator(),NextGens.users,NextGens.manager); FastInv.handlers.getFirst().accept(null); unchanged(active,true,"Repair confirm must honor provider failure");
                    System.out.println("Generator action checks passed: "+checks);
                }
            }
            """;
}
