package com.muhammaddaffa.mdlib.utils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandSender;
public class Config {
 private final FileConfiguration config = new YamlConfiguration();
 public FileConfiguration getConfig() { return config; }
 public void sendMessage(CommandSender player, String key, Placeholder... values) {}
}
