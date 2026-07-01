package com.usainsrht.elytratrails.config;

import com.usainsrht.elytratrails.ElytraTrails;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final ElytraTrails plugin;
    private FileConfiguration guiConfig;
    private FileConfiguration messagesConfig;

    public ConfigManager(ElytraTrails plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // Save default config files if they don't exist
        plugin.saveResource("gui.yml", false);
        plugin.saveResource("messages.yml", false);

        // Load / reload YAML configurations
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    // ── Message Helpers ─────────────────────────────────────

    public Component getMessage(String path) {
        String msg = messagesConfig.getString("messages." + path);
        if (msg == null) {
            return MiniMessage.miniMessage().deserialize("<red>Missing message: " + path);
        }
        return MiniMessage.miniMessage().deserialize(msg);
    }

    public Component getMessage(String path, Object... placeholders) {
        String msg = messagesConfig.getString("messages." + path);
        if (msg == null) {
            return MiniMessage.miniMessage().deserialize("<red>Missing message: " + path);
        }
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String target = placeholders[i].toString();
                String replacement = placeholders[i + 1] == null ? "" : placeholders[i + 1].toString();
                msg = msg.replace(target, replacement);
            }
        }
        return MiniMessage.miniMessage().deserialize(msg);
    }

    // ── GUI Helpers ──────────────────────────────────────────

    public Component getGuiComponent(String path, String def) {
        String val = guiConfig.getString(path, def);
        return MiniMessage.miniMessage().deserialize(val != null ? val : def);
    }

    public List<Component> getGuiComponentList(String path) {
        List<String> rawList = guiConfig.getStringList(path);
        if (rawList.isEmpty()) return Collections.emptyList();
        List<Component> componentList = new ArrayList<>();
        for (String line : rawList) {
            componentList.add(MiniMessage.miniMessage().deserialize(line));
        }
        return componentList;
    }
}
