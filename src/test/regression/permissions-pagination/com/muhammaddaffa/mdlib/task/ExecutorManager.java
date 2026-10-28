package com.muhammaddaffa.mdlib.task;
public class ExecutorManager {
 public static Runnable tick;
 public static long delay, period;
 private static final Provider PROVIDER = new Provider();
 public static Provider getProvider() { return PROVIDER; }
 public static class Provider {
  public Object syncTimer(long initial, long repeat, Runnable task) {
   delay=initial; period=repeat; tick=task; return null;
  }
 }
}
