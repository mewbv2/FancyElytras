package io.mewb.fancyElytras.utils;

import io.mewb.fancyElytras.FancyElytras;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageUtil {

    private final FancyElytras plugin;
    private FileConfiguration messagesConfig;

    public MessageUtil(FancyElytras plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.messagesConfig = plugin.getConfigManager().getMessagesConfig();
    }

public String getMessage(String path) {
    String message = messagesConfig.getString(path);
    
    if (message == null) {
        plugin.getLogger().warning("Missing message: " + path);
        return "&cMissing message: " + path; // Return raw text, don't call formatMessage
    }
    
    return formatMessage(message);
}

public String formatMessage(String message) {
    if (message == null) return "";
    
    // Apply color codes
    message = ChatColor.translateAlternateColorCodes('&', message);
    
    // Apply prefix ONLY if the message doesn't already contain it
    String prefix = messagesConfig.getString("general.prefix", "");
    if (!prefix.isEmpty() && !message.contains(prefix) && !message.startsWith("§")) {
        prefix = ChatColor.translateAlternateColorCodes('&', prefix);
        message = prefix + message;
    }
    
    return message;
}

    public void sendMessage(CommandSender sender, String messagePath) {
        String message = getMessage(messagePath);
        String prefix = getMessage("general.prefix");
        sender.sendMessage(prefix + message);
    }

    public void sendMessage(Player player, String message) {
        if (message.contains("§") || message.contains("&")) {
            // This is a raw message, format and send
            player.sendMessage(formatMessage(message));
        } else {
            // This is a message path, get from config
            sendMessage((CommandSender) player, message);
        }
    }

    public void sendInvalidArguments(CommandSender sender, String usage) {
        String message = getMessage("general.invalid-arguments").replace("{usage}", usage);
        String prefix = getMessage("general.prefix");
        sender.sendMessage(prefix + message);
    }


    public void sendRawMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        
        // Format the message with colors
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        sender.sendMessage(formattedMessage);
    }
}