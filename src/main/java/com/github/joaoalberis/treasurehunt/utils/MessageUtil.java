package com.github.joaoalberis.treasurehunt.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;

public class MessageUtil {

    private static FileConfiguration messages;
    private static String prefix = ChatColor.GOLD + "TreasureHunt » " + ChatColor.YELLOW;

    public static void init(Plugin plugin) {
        plugin.saveResource("messages.yml", false);
        File f = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(f);

        if (messages.contains("prefix")) {
            prefix = ChatColor.translateAlternateColorCodes('&', messages.getString("prefix"));
        }
    }

    public static void reload(Plugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(f);
        if (messages.contains("prefix")) {
            prefix = ChatColor.translateAlternateColorCodes('&', messages.getString("prefix"));
        }
    }

    public static String getRaw(String path) {
        if (messages != null && messages.contains(path)) {
            return ChatColor.translateAlternateColorCodes('&', messages.getString(path));
        }
        return ChatColor.RED + "Missing message: " + path;
    }

    public static String get(String path, Map<String, String> placeholders) {
        String msg = getRaw(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return msg;
    }

    public static String prefix() {
        return prefix;
    }

    public static void send(CommandSender sender, String path) {
        sender.sendMessage(prefix + getRaw(path));
    }

    public static void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefix + get(path, placeholders));
    }
}
