package com.muhammaddaffa.nextgens.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import org.jetbrains.annotations.NotNull;

public abstract class GensRunnable implements Runnable {

    private HandleTask handleTask;

    public synchronized boolean isCancelled() {
        this.checkHandleTask();
        return this.handleTask.isCancelled();
    }

    public synchronized void cancel() {
        this.checkHandleTask();
        this.handleTask.cancel();
    }

    private void checkHandleTask() {
        if (handleTask == null) {
            throw new IllegalStateException("Task not started");
        }
    }

    public synchronized HandleTask runTaskTimerAsynchronously(@NotNull Object plugin, long delay, long period) {
        this.isRunning();
        return this.handleTask = ExecutorManager.getProvider().asyncTimer(delay, period, this);
    }

    public synchronized HandleTask runTaskTimer(@NotNull Object plugin, long delay, long period) {
        this.isRunning();
        return this.handleTask = ExecutorManager.getProvider().syncTimer(delay, period, this);
    }

    public synchronized void isRunning() {
        if (handleTask != null) {
            throw new IllegalStateException("Task already running");
        }
    }

    @NotNull
    public synchronized HandleTask getHandleTask() {
        return handleTask;
    }

    public synchronized void setHandleTask(@NotNull HandleTask handleTask) {
        this.handleTask = handleTask;
    }
}
