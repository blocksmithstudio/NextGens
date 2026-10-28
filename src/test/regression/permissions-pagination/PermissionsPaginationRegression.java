import com.muhammaddaffa.nextgens.autosell.*;
import com.muhammaddaffa.nextgens.sell.multipliers.providers.PermissionSellMultiplierProvider;
import com.muhammaddaffa.nextgens.gui.helpers.Pagination;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.Utils;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class PermissionsPaginationRegression {
 static Set<PermissionAttachmentInfo> permissions = Set.of();
 static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
 static Player player;
 static int assertions;
 static void check(boolean condition, String message) {
  assertions++;
  if (!condition) throw new AssertionError(message);
 }
 static void permissions(Object... nodes) {
  Set<PermissionAttachmentInfo> values = new LinkedHashSet<>();
  for(int i=0;i<nodes.length;i+=2) values.add(new PermissionAttachmentInfo(player,(String)nodes[i],null,(boolean)nodes[i+1]));
  permissions=values;
 }
 public static void main(String[] args) {
  player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class[]{Player.class},(proxy,method,params)->{
   return switch(method.getName()) {
    case "getEffectivePermissions" -> permissions;
    case "getUniqueId" -> ID;
    case "hasPermission" -> false;
    case "toString" -> "regression-player";
    case "hashCode" -> 1;
    case "equals" -> proxy==params[0];
    default -> null;
   };
  });
  permissions("nextgens.autosell.inv.20",false,"nextgens.autosell.inv",false,"nextgens.autosell.invalid.1",true,"nextgens.autosell.inv.*",true,"nextgens.autosell.inv.-1",true,"nextgens.autosell.inv.0",true,"nextgens.autosell.inv.999999999999",true);
  check(!Autosell.hasAutosellInventoryPermission(player),"denied and malformed autosell nodes must not authorize");
  check(Autosell.getAutosellInventoryInterval(player)==0,"malformed interval ignored");
  permissions("nextgens.autosell.inv",true);
  check(Autosell.hasAutosellInventoryPermission(player),"explicitly granted base node retains compatibility");
  permissions("nextgens.autosell.inv",false);
  check(!Autosell.hasAutosellInventoryPermission(player),"explicitly denied base node cannot grant autosell");
  permissions("nextgens.autosell.inv.20",true,"nextgens.autosell.inv.5",true,"nextgens.autosell.inv.1",false);
  check(Autosell.hasAutosellInventoryPermission(player),"valid autosell node recognized");
  check(Autosell.getAutosellInventoryInterval(player)==5,"smallest positive granted interval selected");
  permissions("nextgens.multiplier.sell.9",false,"nextgens.multiplier.sell.2_5",true,"nextgens.multiplier.sell.Infinity",true,"nextgens.multiplier.sell.NaN",true,"nextgens.multiplier.sell.1e999",true,"nextgens.multiplier.sell.*",true,"nextgens.multiplier.sellExtra.99",true);
  var multiplier = new PermissionSellMultiplierProvider();
  check(multiplier.getMultiplier(player,null,null)==2.5,"denied, malformed, nonfinite multipliers ignored");
  permissions("nextgens.multiplier.sell.Infinity",true,"nextgens.multiplier.sell.-2",true);
  check(multiplier.getMultiplier(player,null,null)==0,"only positive finite multiplier accepted");
  permissions("nextgens.cashback.95",false,"nextgens.cashback.5",true,"nextgens.cashback.*",true,"nextgens.cashback.9999999999999",true,"nextgens.cashback.20.extra",true,"nextgens.cashback.-4",true);
  check(Utils.getCashback(player)==5,"cashback denial and malformed nodes ignored");
  var config=NextGens.DEFAULT_CONFIG.getConfig();
  config.set("default-max-generator.enabled",true);config.set("default-max-generator.amount",10);
  UserManager manager=new UserManager();
  manager.getUser(player).setBonus(5);
  permissions("nextgens.max.999",false,"nextgens.max.40",true,"nextgens.max.*",true,"nextgens.max.9999999999999",true,"nextgens.max.100.extra",true);
  check(manager.getMaxSlot(player)==45,"max quota ignores denied and malformed nodes and retains bonus");
  config.set("player-generator-limit.enabled",true);config.set("player-generator-limit.limit",30);
  check(manager.getMaxSlot(player)==30,"configured quota cap preserved");
  config.set("player-generator-limit.enabled",false);
  permissions("nextgens.max.999",false);
  check(manager.getMaxSlot(player)==15,"default quota and bonus survive denied node");
  for (int capacity : new int[]{1,2,9,45}) {
   List<Integer> slots=IntStream.range(0,capacity).boxed().toList();
   for (int size : new int[]{0,1,capacity,capacity+1,capacity*2,capacity*2+1}) {
    List<Integer> items=IntStream.range(0,size).boxed().toList();
    var pagination=new Pagination<>(items,slots);
    check(pagination.totalPage==Math.max(1,(size+capacity-1)/capacity),"correct total pages");
    List<Integer> actual=new ArrayList<>();
    for (int page=1;page<=pagination.totalPage;page++) {
     pagination.currentPage=page;
     check(pagination.getItems()!=null,"every page including last singleton exists");
     actual.addAll(pagination.getItems());
     check(pagination.hasPreviousPage()==(page>1),"previous navigation");
     check(pagination.hasNextPage()==(page<pagination.totalPage),"next navigation");
    }
    check(actual.equals(items),"all page items preserved exactly once");
   }
  }
  check(new Pagination<>(List.of(1),List.<Integer>of()).getItems().isEmpty(),"empty slot configuration does not divide by zero");
  Server server=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class[]{Server.class},(proxy,method,params)->switch(method.getName()) {
   case "getOnlinePlayers" -> List.of(player);
   case "getLogger" -> Logger.getLogger("regression");
   case "getName", "getVersion", "getBukkitVersion" -> "regression";
   default -> null;
  });
  Bukkit.setServer(server);
  config.set("autosell.inventory-interval",20);
  User user=manager.getUser(player);user.setToggleInventoryAutoSell(true);
  permissions("nextgens.autosell.inv.20",true);
  new AutosellManager(manager).startTask();
  check(ExecutorManager.delay==20&&ExecutorManager.period==20,"timer advances in full seconds");
  for(int i=0;i<19;i++) ExecutorManager.tick.run();
  check(NextGens.getInstance().getSellManager().sales==0,"no sale before configured interval");
  ExecutorManager.tick.run();
  check(NextGens.getInstance().getSellManager().sales==1&&user.getInterval()==0,"sale at interval then reset");
  for(int i=0;i<20;i++) ExecutorManager.tick.run();
  check(NextGens.getInstance().getSellManager().sales==2,"second interval");
  permissions("nextgens.autosell.inv.20",false);
  user.setInterval(19);for(int i=0;i<25;i++) ExecutorManager.tick.run();
  check(NextGens.getInstance().getSellManager().sales==2&&user.getInterval()==0,"revocation stops pending and recurring sales");
  permissions("nextgens.autosell.inv.20",true);
  user.setToggleInventoryAutoSell(false);user.setInterval(19);ExecutorManager.tick.run();
  check(NextGens.getInstance().getSellManager().sales==2&&user.getInterval()==0,"disabled toggle resets elapsed time");
  user.setToggleInventoryAutoSell(true);config.set("autosell.inventory-interval",0);ExecutorManager.tick.run();
  check(NextGens.getInstance().getSellManager().sales==3,"invalid nonpositive config uses safe one-second minimum");
  System.out.println("PASS: "+assertions+" permission, autosell timing, and pagination checks against modified source classes");
 }
}
