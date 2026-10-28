package com.muhammaddaffa.nextgens;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.nextgens.sell.SellManager;
public class NextGens {
 public static final Config DEFAULT_CONFIG = new Config();
 private static final NextGens INSTANCE = new NextGens();
 private final SellManager sales = new SellManager();
 public static NextGens getInstance() { return INSTANCE; }
 public SellManager getSellManager() { return sales; }
}
