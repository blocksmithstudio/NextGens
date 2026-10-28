package com.muhammaddaffa.nextgens;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.ExecutorProvider;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.discord.DiscordExecute;
import com.muhammaddaffa.nextgens.events.Event;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.*;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.generators.runnables.*;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.Settings;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import sun.misc.Unsafe;

public final class LifecycleRegression {
    static int checks;
    static Unsafe unsafe;
    static final List<Runnable> regionTasks = new ArrayList<>();
    static final List<Runnable> asyncTasks = new ArrayList<>();
    static final AtomicInteger cancellations = new AtomicInteger();
    static final AtomicInteger blockReads = new AtomicInteger();
    static boolean rejectScheduler;
    static boolean chunkLoaded = true;
    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
    static void field(Class<?> type, Object owner, String name, Object value) throws Exception {
        Field f=type.getDeclaredField(name); f.setAccessible(true); f.set(owner,value);
    }
    static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type==boolean.class) return false;
        if (type==int.class) return 0;
        if (type==long.class) return 0L;
        if (type==double.class) return 0D;
        if (type==float.class) return 0F;
        if (type==short.class) return (short)0;
        if (type==byte.class) return (byte)0;
        if (type==char.class) return (char)0;
        return null;
    }
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);
    }
    static Config config(YamlConfiguration yaml) throws Exception {
        Config c=(Config)unsafe.allocateInstance(Config.class);
        field(Config.class,c,"config",yaml);
        return c;
    }
    @SuppressWarnings("unchecked")
    static Map<String,ActiveGenerator> registry(GeneratorManager manager) throws Exception {
        Field f=GeneratorManager.class.getDeclaredField("activeGenerators"); f.setAccessible(true);
        return (Map<String,ActiveGenerator>)f.get(manager);
    }
    static Generator generator(String id) {
        return new Generator(id,id,20,null,List.of(),null,0,true,0,100,false,List.of(),List.of());
    }
    static List<?> selection(CorruptionTask task) throws Exception {
        Method m=CorruptionTask.class.getDeclaredMethod("getPossiblyInfectedGenerators");m.setAccessible(true);
        return (List<?>)m.invoke(task);
    }
    public static void main(String[] args) throws Exception {
        Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);unsafe=(Unsafe)uf.get(null);
        NextGens plugin=(NextGens)unsafe.allocateInstance(NextGens.class);
        Server loggingServer = proxy(Server.class, (p, m, a) -> m.getName().equals("getLogger")
                ? Logger.getLogger("lifecycle-regression") : defaultValue(m.getReturnType()));
        field(JavaPlugin.class, plugin, "server", loggingServer);
        field(JavaPlugin.class, plugin, "description", new org.bukkit.plugin.PluginDescriptionFile("LifecycleRegression", "1.0", "fixture.Main"));
        field(JavaPlugin.class, plugin, "logger", new org.bukkit.plugin.PluginLogger(plugin));
        field(NextGens.class,null,"instance",plugin);
        field(MDLib.class,null,"instance",plugin);
        ExecutorProvider provider=proxy(ExecutorProvider.class,(p,m,a)->{
            if (m.getName().equals("asyncTimer")) {
                if(rejectScheduler)throw new UnsupportedOperationException("fixture scheduler rejection");
                if (a[2] instanceof GeneratorTask) {
                    check((long)a[0]==20L && (long)a[1]==5L,"generator starts after 20 ticks and repeats every 5 ticks");
                } else {
                    check((long)a[0]==1L && (long)a[1]==60L,"Discord timing preserved");
                }
                asyncTasks.add((Runnable)a[2]);
            } else if(m.getName().equals("region")) regionTasks.add((Runnable)a[3]);
            else throw new AssertionError("Unexpected scheduler method "+m.getName());
            return new HandleTask(proxy(BukkitTask.class,(t,method,values)->{
                if(method.getName().equals("cancel"))cancellations.incrementAndGet();
                return defaultValue(method.getReturnType());
            }));
        });
        field(ExecutorManager.class,null,"provider",provider);
        corruption();
        discord(plugin);
        generation();
        System.out.println("PASS: "+checks+" lifecycle regression checks");
    }
    static void corruption() throws Exception {
        GeneratorManager manager=new GeneratorManager(null);Map<String,ActiveGenerator> active=registry(manager);
        CorruptionTask task=new CorruptionTask(manager);
        Settings.CORRUPTION_PERCENTAGE=100;Settings.CORRUPTION_BLACKLISTED_GENERATORS=List.of("blocked");
        active.put("blocked",new ActiveGenerator(UUID.randomUUID(),null,generator("blocked")));
        check(selection(task).isEmpty(),"blacklisted-only selection terminates");
        ActiveGenerator allowed=new ActiveGenerator(UUID.randomUUID(),null,generator("allowed"));active.put("allowed",allowed);
        check(selection(task).equals(List.of(allowed)),"unreachable quota clamped to eligible count");
        Settings.CORRUPTION_PERCENTAGE=50;
        check(selection(task).equals(List.of(allowed)),"quota preserves percentage of noncorrupted population");
        Settings.CORRUPTION_PERCENTAGE=-5;check(selection(task).isEmpty(),"negative percentage bounded");
        Settings.CORRUPTION_PERCENTAGE=200;check(selection(task).equals(List.of(allowed)),"excess percentage bounded");
        allowed.setCorrupted(true);check(selection(task).isEmpty(),"already corrupted candidates excluded");
        active.clear();check(selection(task).isEmpty(),"empty population terminates");
    }
    static void discord(JavaPlugin plugin) throws Exception {
        YamlConfiguration webhook=new YamlConfiguration();webhook.set("settings.send-interval-ticks",60);
        webhook.set("webhook-event.end-message","Fixture ended");
        NextGens.WEBHOOK_CONFIG=config(webhook);
        DiscordExecute.sendDiscordNotification(plugin,"Fixture one");
        DiscordExecute.sendDiscordNotification(plugin,"Fixture two");
        check(asyncTasks.size()==1,"single worker through MDLib scheduler");
        Field queueField=DiscordExecute.class.getDeclaredField("messageQueue");queueField.setAccessible(true);
        Queue<?> queue=(Queue<?>)queueField.get(null);queue.clear();
        asyncTasks.get(0).run();check(cancellations.get()==1,"idle worker cancels via HandleTask");
        rejectScheduler=true;
        YamlConfiguration events=new YamlConfiguration();events.set("events.wait-time",20);NextGens.EVENTS_CONFIG=config(events);
        YamlConfiguration fixture=new YamlConfiguration();fixture.set("event.display-name","Fixture");fixture.set("event.duration",0);
        EventManager manager=new EventManager();
        field(EventManager.class,manager,"activeEvent",new Event("fixture",Event.Type.CUSTOM,fixture,"event"));
        Method expiry=EventManager.class.getDeclaredMethod("whenEventIsRunning");expiry.setAccessible(true);expiry.invoke(manager);
        check(manager.getActiveEvent()==null,"automatic expiry survives notification scheduler failure");
        field(EventManager.class,manager,"activeEvent",new Event("fixture",Event.Type.CUSTOM,fixture,"event"));
        check(manager.forceEnd() && manager.getActiveEvent()==null,"forced end survives notification scheduler failure");
        queue.clear();rejectScheduler=false;
    }
    static void generation() throws Exception {
        PluginManager pluginManager=proxy(PluginManager.class,(p,m,a)->defaultValue(m.getReturnType()));
        Server server=proxy(Server.class,(p,m,a)->m.getName().equals("getPluginManager")?pluginManager:defaultValue(m.getReturnType()));
        field(Bukkit.class,null,"server",server);
        Block block=proxy(Block.class,(p,m,a)->defaultValue(m.getReturnType()));
        World world=proxy(World.class,(p,m,a)->switch(m.getName()){
            case "getName" -> "fixture";
            case "isChunkLoaded" -> chunkLoaded;
            case "getBlockAt" -> {blockReads.incrementAndGet();yield block;}
            default -> defaultValue(m.getReturnType());
        });
        GeneratorManager manager=new GeneratorManager(null);Map<String,ActiveGenerator> active=registry(manager);
        Generator gen=generator("fixture");Location location=new Location(world,0,64,0);
        ActiveGenerator ag=new ActiveGenerator(UUID.randomUUID(),location,gen);
        String key=com.muhammaddaffa.mdlib.utils.LocationUtils.serialize(location);active.put(key,ag);
        Settings.BLACKLISTED_WORLDS=List.of();Settings.CORRUPTION_ENABLED=false;Settings.FORCE_UPDATE_BLOCKS=false;
        UserManager users=new UserManager(){@Override public User getUser(UUID uuid){return null;}};
        EventManager events=new EventManager();
        GeneratorTask.start(manager,events,users);
        GeneratorTask task=(GeneratorTask)asyncTasks.get(asyncTasks.size()-1);
        for(int i=0;i<79;i++)task.run();check(regionTasks.isEmpty(),"no premature drop");
        task.run();check(regionTasks.size()==1 && ag.getTimer()==20,"80 quarter-second callbacks queue one drop");
        regionTasks.remove(0).run();check(blockReads.get()==1 && ag.getTimer()==0,"valid callback executes and resets timer");
        Event speed=new Event("speed",Event.Type.GENERATOR_SPEED,new YamlConfiguration(),"event").setSpeedMultiplier(87.5);
        field(EventManager.class,events,"activeEvent",speed);
        for(int i=0;i<9;i++)task.run();check(regionTasks.isEmpty(),"no drop before boosted 2.5-second interval");
        task.run();check(regionTasks.size()==1 && ag.getTimer()==2.5,"speed boost preserves quarter-second precision");
        regionTasks.remove(0).run();check(ag.getTimer()==0,"boosted drop resets timer");
        field(EventManager.class,events,"activeEvent",null);
        blockReads.set(1);
        ag.setTimer(19.75);task.run();active.remove(key);regionTasks.remove(0).run();check(blockReads.get()==1,"removed generator callback cannot access blocks");
        active.put(key,ag);ag.setTimer(19.75);task.run();active.put(key,new ActiveGenerator(UUID.randomUUID(),location,gen));regionTasks.remove(0).run();check(blockReads.get()==1,"replacement generator callback cannot access blocks");
        active.put(key,ag);ag.setTimer(19.75);task.run();ag.setGenerator(generator("upgraded"));regionTasks.remove(0).run();check(blockReads.get()==1,"retiered generator callback cannot restore stale block");
        ag.setGenerator(gen);ag.setTimer(19.75);task.run();chunkLoaded=false;regionTasks.remove(0).run();check(blockReads.get()==1,"unloaded chunk callback cannot access blocks");
        chunkLoaded=true;ag.setTimer(19.75);task.run();Settings.CORRUPTION_ENABLED=true;ag.setCorrupted(true);regionTasks.remove(0).run();check(blockReads.get()==1,"newly corrupted generator callback cannot drop");
    }
}
