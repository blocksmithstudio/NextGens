package com.muhammaddaffa.nextgens.events.managers;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.events.Event;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class EventManager {

    private final List<Event> eventList = new ArrayList<>();

    private Event activeEvent;
    private int index;
    private Double waitTime;

    public List<Event> getEvents() {
        return new ArrayList<>(this.eventList);
    }

    public List<String> getEventName() {
        return this.eventList.stream().map(Event::getId).collect(Collectors.toList());
    }

    @Nullable
    public Event getEvent(String id) {
        return this.eventList.stream()
                .filter(event -> event.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public Event getRandomEvent() {
        List<Event> list = new ArrayList<>(this.eventList);
        // shuffle the list
        Collections.shuffle(list);
        // get random event
        Event event = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        // check for chance
        // check for is only command
        if (ThreadLocalRandom.current().nextDouble(100.0) < event.getChance() || event.isOnlyByCommand()) {
            return this.getRandomEvent();
        }
        // check for same event
        if (this.activeEvent != null && event.getId().equals(this.activeEvent.getId())) {
            return this.getRandomEvent();
        }
        return event;
    }

    @NotNull
    public Event getNextEvent(boolean count) {
        int next = this.index + 1;
        // check if the next event is existed or not
        if (!Common.isValid(this.eventList, next)) {
            // assign the index to the first one
            if (count) {
                this.index = 0;
            }
            // if there is no next event
            // return the first event
            return this.eventList.get(0).clone();
        }
        // assign the index to the next event
        if (count) {
            this.index = next;
        }
        // if the next event is present, check for availability
        Event event = this.eventList.get(next);
        if (event.isOnlyByCommand()) {
            return this.getNextEvent(count).clone();
        }
        return event.clone();
    }

    public void loadEvents() {
        // clear the event list first
        this.eventList.clear();
        // get all variables we want
        FileConfiguration config = NextGens.EVENTS_CONFIG.getConfig();
        // check if there are any events or not
        if (!config.isConfigurationSection("events.events")) {
            return;
        }
        // loop through all events
        for (String id : config.getConfigurationSection("events.events").getKeys(false)) {
            // create the event object
            Event event = Event.createEvent(config, "events.events." + id, id);
            // if the event is not valid, skip it
            if (event == null) {
                Logger.warning("Failed to load event '"+ id + "' because the configuration is invalid!");
                continue;
            }
            // cache it
            this.eventList.add(event);
        }
        // log message
        Logger.info("Successfully loaded " + this.eventList.size() + " events!");
    }

    public void refresh() {
        if (this.activeEvent == null) {
            return;
        }
        String eventId = this.activeEvent.getId();
        double duration = this.activeEvent.getDuration();
        // get the new event object
        Event refreshed = this.getEvent(eventId);
        // end the event if the event is not exist
        if (refreshed == null) {
            this.forceEnd();
        } else {
            this.activeEvent = refreshed;
            this.activeEvent.setDuration(duration);
        }
    }

    public void startTask() {
        // set the default wait time
        if (this.waitTime == null) {
            this.waitTime = this.getDefaultWaitTime();
        }

        ExecutorManager.getProvider().asyncTimer(20L, 2L, () -> {
            // if the event is not enabled, don't bother it
            if (!this.isEnabled()) {
                return;
            }
            if (this.activeEvent == null) {
                this.whenEventIsOnCooldown();
            } else {
                //a
                this.whenEventIsRunning();

            }
        });
    }

    private void whenEventIsOnCooldown() {
        // If an event is running, reset wait time
        if (this.activeEvent != null) {
            this.waitTime = this.getDefaultWaitTime();
            return;
        }

        // Ensure wait time is initialized
        if (this.waitTime == null) {
            this.waitTime = this.getDefaultWaitTime();
        }

        // Check if wait time is below or equals to 0
        if (this.waitTime <= 0) {
            // Reset the wait time
            this.waitTime = this.getDefaultWaitTime();

            // Assign the next event
            this.activeEvent = this.isRandom()
                    ? this.getRandomEvent().clone()
                    : this.getNextEvent(true).clone();

            // Send start messages
            if (this.activeEvent != null) {
                this.activeEvent.sendStartMessage();
            }

            return;
        }

        // Decrease the wait time
        this.waitTime = Math.max(0, this.waitTime - 0.1); // Avoid negative wait time
    }

    private void whenEventIsRunning() {
        // If no event is running, return early
        if (this.activeEvent == null) {
            return;
        }

        // Ensure duration is properly set
        if (this.activeEvent.getDuration() <= 0) {
            // End the current event
            this.activeEvent.sendEndMessage();
            this.activeEvent = null;
            return;
        }

        // Decrease the duration
        this.activeEvent.setDuration(Math.max(0, this.activeEvent.getDuration() - 0.1)); // Avoid negative duration
    }

    public void forceStart(Event event) {
        // assign the active event
        this.activeEvent = event.clone();
        // send start messages
        this.activeEvent.sendStartMessage();
        // reset back the wait time
        this.waitTime = this.getDefaultWaitTime();
    }

    public boolean forceEnd() {
        if (this.activeEvent == null) {
            return false;
        }
        this.activeEvent.sendEndMessage();
        this.activeEvent = null;
        this.waitTime = this.getDefaultWaitTime();
        return true;
    }

    public void load() {
        // get the config
        FileConfiguration config = NextGens.DATA_CONFIG.getConfig();
        // get the saved data
        String eventId = config.getString("events.id");
        if (eventId != null && this.getEvent(eventId) != null) {
            Event event = this.getEvent(eventId);
            event.setDuration(config.getDouble("events.timer"));
            // set the active event to this one
            this.activeEvent = event;
        }
        int waitTime = config.getInt("event-wait-time");
        int index = config.getInt("event-index");
        // assign the variables
        this.waitTime = config.get("event-wait-time") == null ? this.getDefaultWaitTime() : waitTime;
        this.index = config.get("event-index") == null ? -1 : index;
    }
    public void save() {
        // get variables
        Config data = NextGens.DATA_CONFIG;
        FileConfiguration config = data.getConfig();
        // save it
        if (this.activeEvent == null) {
            config.set("events", null);
        } else {
            config.set("events.id", this.activeEvent.getId());
            config.set("events.timer", this.activeEvent.getDuration());
        }
        config.set("event-wait-time", this.waitTime);
        config.set("event-index", this.index);
        // finally, save the config
        data.saveConfig();
    }

    public Event getActiveEvent() {
        return activeEvent;
    }

    public boolean isEnabled() {
        return NextGens.EVENTS_CONFIG.getBoolean("events.enabled");
    }

    public boolean isRandom() {
        return NextGens.EVENTS_CONFIG.getBoolean("events.random");
    }

    public double getWaitTime() {
        return this.waitTime;
    }

    public double getDefaultWaitTime() {
        return NextGens.EVENTS_CONFIG.getDouble("events.wait-time");
    }

}
