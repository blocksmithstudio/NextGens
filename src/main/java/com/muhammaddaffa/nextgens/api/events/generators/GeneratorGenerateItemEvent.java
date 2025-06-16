package com.muhammaddaffa.nextgens.api.events.generators;

import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Drop;
import com.muhammaddaffa.nextgens.generators.Generator;

import java.util.UUID;

public class GeneratorGenerateItemEvent extends GeneratorEvent{

    private final ActiveGenerator activeGenerator;
    private Drop drop;
    private int dropAmount;
    private boolean dropItem = true;

    public GeneratorGenerateItemEvent(Generator generator, ActiveGenerator activeGenerator, Drop drop, int dropAmount) {
        super(generator);
        this.activeGenerator = activeGenerator;
        this.drop = drop;
        this.dropAmount = dropAmount;
    }

    public ActiveGenerator getActiveGenerator() {
        return activeGenerator;
    }

    public Drop getDrop() {
        return drop;
    }

    public void setDrop(Drop drop) {
        this.drop = drop;
    }

    public int getDropAmount() {
        return dropAmount;
    }

    public void setDropAmount(int dropAmount) {
        this.dropAmount = dropAmount;
    }

    public boolean isDropItem() {
        return dropItem;
    }

    public void setDropItem(boolean dropItem) {
        this.dropItem = dropItem;
    }

    public UUID getOwner() {
        return this.activeGenerator.getOwner();
    }

    public double getTimer() {
        return this.activeGenerator.getTimer();
    }

    public void setTimer(double timer) {
        this.activeGenerator.setTimer(timer);
    }

}
