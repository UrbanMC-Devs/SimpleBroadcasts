package net.urbanmc.simplebroadcasts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class SimpleBroadcasts extends JavaPlugin {

    private List<BaseComponent[]> messages = Collections.emptyList();
    private int secondsInterval;
    private int messageIndex;

    @Override
    public void onEnable() {
        createConfigIfNotExists();
        loadConfig();
        if (!messages.isEmpty())
            initTask();

        getCommand("simplebroadcasts").setExecutor(this);
    }

    private void createConfigIfNotExists() {
        final File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.getParentFile().exists())
            configFile.getParentFile().mkdir();

        if (!configFile.exists()) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("config.yml");
            try {
                Files.copy(is, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error creating config!", e);
            }
        }
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        secondsInterval = config.getInt("interval", 300);

        String prefix = config.getString("prefix", "");
        List<String> list = config.getStringList("messages");
        messages = new ArrayList<>();

        for (String message : list) {
            if (message == null)
                continue;

            final Component kyoriComponent = MiniMessage.get().parse(prefix + message);
            // Store the messages as basecomponents for optimization purposes
            BaseComponent[] components = BungeeComponentSerializer.get().serialize(kyoriComponent);

            messages.add(components);
        }
    }

    private void initTask() {
        messageIndex = 0;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            BaseComponent[] components = messages.get(messageIndex++);

            // Send to console as well
            Bukkit.getConsoleSender().spigot().sendMessage(components);
            // Send to players
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(components);
            }

            // Verify message index
            if (messageIndex >= messages.size())
                messageIndex = 0;
        }, 0, secondsInterval * 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simplebroadcasts.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.RED + "Usage: /simplebroadcasts reload");
            return true;
        }

        Bukkit.getScheduler().cancelTasks(this);
        reloadConfig();
        loadConfig();

        if (!messages.isEmpty())
            initTask();

        sender.sendMessage(ChatColor.GREEN + "Reloaded configuration for simple broadcasts!");
        return true;
    }
}
