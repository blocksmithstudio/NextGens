package com.muhammaddaffa.nextgens.discord;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordExecute {

    private static final int MAX_DELIVERY_ATTEMPTS = 3;
    private static final int CONTENT_CHARACTER_LIMIT = 2_000;
    private static final int EMBED_DESCRIPTION_CHARACTER_LIMIT = 4_096;

    private static final Queue<QueuedMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean queueWorkerRunning = new AtomicBoolean(false);

    private DiscordExecute() {
    }

    public static void sendDiscordNotification(JavaPlugin plugin, String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        for (int start = 0; start < message.length(); start += CONTENT_CHARACTER_LIMIT) {
            int end = Math.min(start + CONTENT_CHARACTER_LIMIT, message.length());
            messageQueue.add(new QueuedMessage(message.substring(start, end), 0));
        }

        if (queueWorkerRunning.compareAndSet(false, true)) {
            long interval = Math.max(1L, NextGens.WEBHOOK_CONFIG.getInt(
                    "settings.send-interval-ticks",
                    60
            ));

            try {
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task -> {
                    if (messageQueue.isEmpty()) {
                        queueWorkerRunning.set(false);

                        // A producer may have queued a message while the worker was stopping.
                        if (messageQueue.isEmpty() || !queueWorkerRunning.compareAndSet(false, true)) {
                            task.cancel();
                        }
                        return;
                    }

                    sendQueuedMessages(plugin);
                }, 1L, interval);
            } catch (RuntimeException exception) {
                queueWorkerRunning.set(false);
                throw exception;
            }
        }
    }

    private static void sendQueuedMessages(JavaPlugin plugin) {
        FileConfiguration config = NextGens.WEBHOOK_CONFIG.getConfig();
        boolean embedEnabled = config.getBoolean("webhook-event.embed.enabled");
        int characterLimit = embedEnabled ? EMBED_DESCRIPTION_CHARACTER_LIMIT : CONTENT_CHARACTER_LIMIT;
        int batchLimit = Math.max(1, config.getInt(
                "settings.max-message-per-batch",
                8
        ));
        List<QueuedMessage> queuedMessages = new ArrayList<>();
        int characterCount = 0;

        while (queuedMessages.size() < batchLimit) {
            QueuedMessage queuedMessage = messageQueue.peek();

            if (queuedMessage == null) {
                break;
            }

            int separatorLength = queuedMessages.isEmpty() ? 0 : 1;
            if (characterCount + separatorLength + queuedMessage.content().length() > characterLimit) {
                break;
            }

            messageQueue.poll();
            queuedMessages.add(queuedMessage);
            characterCount += separatorLength + queuedMessage.content().length();
        }

        if (queuedMessages.isEmpty()) {
            return;
        }

        String webhookUrl = config.getString("webhook-event.url", "");
        if (webhookUrl.isBlank()) {
            requeueOrDiscard(plugin, queuedMessages, "webhook URL is not configured");
            return;
        }

        String message = String.join("\n", queuedMessages.stream().map(QueuedMessage::content).toList());
        DiscordWebHook webhook = new DiscordWebHook(webhookUrl);
        webhook.setUsername(config.getString("webhook-event.username"));
        if (embedEnabled) {
            webhook.addEmbed(new DiscordWebHook.EmbedObject()
                    .setTitle(config.getString("webhook-event.embed.title"))
                    .setDescription(message)
                    .setColor(color(config.getString("webhook-event.embed.color"))));
        } else {
            webhook.setContent(message);
        }

        try {
            webhook.execute();
        } catch (IOException | RuntimeException exception) {
            requeueOrDiscard(plugin, queuedMessages, exception.getMessage());
        }
    }

    private static void requeueOrDiscard(JavaPlugin plugin, List<QueuedMessage> messages, String reason) {
        int discarded = 0;

        for (QueuedMessage message : messages) {
            if (message.attempts() + 1 < MAX_DELIVERY_ATTEMPTS) {
                messageQueue.add(new QueuedMessage(message.content(), message.attempts() + 1));
            } else {
                discarded++;
            }
        }

        plugin.getLogger().warning("Failed to send Discord notification: " + reason);
        if (discarded > 0) {
            plugin.getLogger().warning("Discarded " + discarded + " Discord notification(s) after "
                    + MAX_DELIVERY_ATTEMPTS + " failed attempts.");
        }
    }

    public static Color color(String color) {
        try {
            Field field = Color.class.getField(color.toUpperCase());
            return (Color) field.get(null);
        } catch (Exception e) {
            Logger.warning("Invalid color, reverting to WHITE");
            return Color.WHITE;
        }
    }

    private record QueuedMessage(String content, int attempts) {
    }
}
