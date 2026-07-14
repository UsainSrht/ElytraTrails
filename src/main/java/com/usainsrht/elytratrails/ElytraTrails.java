package com.usainsrht.elytratrails;

import com.usainsrht.elytratrails.command.DynamicCommand;
import com.usainsrht.elytratrails.command.ElytraCommand;
import com.usainsrht.elytratrails.config.ConfigManager;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.config.WorldGuardHook;
import com.usainsrht.elytratrails.economy.VaultHook;
import com.usainsrht.elytratrails.gui.CosmeticsGUI;
import com.usainsrht.elytratrails.gui.TrailGUI;
import com.usainsrht.elytratrails.listener.GUIListener;
import com.usainsrht.elytratrails.listener.PlayerListener;
import com.usainsrht.elytratrails.listener.ProjectileListener;
import com.usainsrht.elytratrails.trail.ParticleTask;
import com.usainsrht.elytratrails.trail.ProjectileTrailTask;
import com.usainsrht.elytratrails.skins.SkinsRestorerHook;
import com.usainsrht.elytratrails.skin.SkinChangePricing;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ElytraTrails extends JavaPlugin {

    private static ElytraTrails instance;

    private ConfigManager configManager;
    private TrailManager trailManager;
    private PlayerDataManager playerDataManager;
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private SkinsRestorerHook skinsRestorerHook;
    private SkinChangePricing skinChangePricing;
    private CosmeticsGUI cosmeticsGUI;
    private TrailGUI trailGUI;
    private ParticleTask particleTask;
    private ProjectileTrailTask projectileTrailTask;

    @Override
    public void onEnable() {
        instance = this;

        // ── Configuration ────────────────────────────────────
        configManager = new ConfigManager(this);
        trailManager = new TrailManager(this);
        playerDataManager = new PlayerDataManager(this);

        // ── Economy ──────────────────────────────────────────
        vaultHook = new VaultHook();
        if (vaultHook.isEnabled()) {
            getLogger().info("Vault economy hooked successfully.");
        } else {
            getLogger().info("Vault not found — trail purchasing disabled.");
        }

        // ── WorldGuard ───────────────────────────────────────
        worldGuardHook = new WorldGuardHook(getLogger());

        // ── SkinsRestorer ────────────────────────────────────
        skinsRestorerHook = new SkinsRestorerHook(this);
        skinChangePricing = new SkinChangePricing(this);

        // ── GUI (build CosmeticsGUI after TrailGUI; resolve circular ref) ──
        // TrailGUI needs CosmeticsGUI for the Back button, so we construct
        // CosmeticsGUI first with a placeholder, then inject.
        // Solution: TrailGUI receives cosmeticsGUI as a field we set post-construction.
        trailGUI = new TrailGUI(this, trailManager, playerDataManager, vaultHook, null);
        cosmeticsGUI = new CosmeticsGUI(this, trailGUI);
        trailGUI.setCosmeticsGUI(cosmeticsGUI);

        // ── Particle task (runs every tick) ──────────────────
        particleTask = new ParticleTask(this, trailManager, playerDataManager, worldGuardHook);
        particleTask.runTaskTimer(this, 0L, 1L);

        // ── Projectile trail task (runs every tick) ──────────
        projectileTrailTask = new ProjectileTrailTask(this);
        projectileTrailTask.runTaskTimer(this, 0L, 1L);

        // ── Listeners ────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new GUIListener(cosmeticsGUI, trailGUI), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(playerDataManager), this);
        getServer().getPluginManager().registerEvents(
                new ProjectileListener(playerDataManager, trailManager, projectileTrailTask), this);

        // ── Commands ─────────────────────────────────────────
        registerCommands();

        getLogger().info("ElytraTrails v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        unregisterCommands();
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (projectileTrailTask != null) {
            projectileTrailTask.cancel();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (skinsRestorerHook != null) {
            skinsRestorerHook.disable();
        }
        getLogger().info("ElytraTrails disabled.");
    }

    private ElytraCommand elytraCommand;

    public void registerCommands() {
        unregisterCommands();

        FileConfiguration config = getConfig();
        String commandName = config.getString("command.name", "elytra");
        List<String> commandAliases = config.getStringList("command.aliases");
        if (commandAliases.isEmpty()) {
            commandAliases = List.of("elytratrails", "et", "cosmetics");
        }

        if (elytraCommand == null) {
            elytraCommand = new ElytraCommand(
                    this, trailManager, playerDataManager, cosmeticsGUI, trailGUI);
        }
        elytraCommand.loadSubcommands();

        DynamicCommand dynamicCommand = new DynamicCommand(
                commandName,
                "Main ElytraTrails cosmetics command.",
                "/" + commandName + " [gui|elytra|player|arrow|reload|give]",
                commandAliases,
                elytraCommand
        );

        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            commandMap.register("elytratrails", dynamicCommand);
        }
    }

    public void unregisterCommands() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) return;
        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        if (knownCommands == null) return;

        Set<DynamicCommand> commandsToRemove = new LinkedHashSet<>();
        synchronized (knownCommands) {
            for (Command command : knownCommands.values()) {
                if (command instanceof DynamicCommand dynamicCommand) {
                    commandsToRemove.add(dynamicCommand);
                }
            }
        }
        for (DynamicCommand command : commandsToRemove) {
            command.unregister(commandMap);
        }
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(getServer());
        } catch (Exception e) {
            getLogger().severe("Could not retrieve Bukkit CommandMap: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            return (Map<String, Command>) knownCommandsField.get(commandMap);
        } catch (Exception e) {
            try {
                Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                return (Map<String, Command>) knownCommandsField.get(commandMap);
            } catch (Exception ex) {
                getLogger().severe("Could not retrieve knownCommands map: " + ex.getMessage());
                return null;
            }
        }
    }

    // ── Accessors ────────────────────────────────────────────

    public static ElytraTrails getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TrailManager getTrailManager() {
        return trailManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public CosmeticsGUI getCosmeticsGUI() {
        return cosmeticsGUI;
    }

    public TrailGUI getTrailGUI() {
        return trailGUI;
    }

    public ProjectileTrailTask getProjectileTrailTask() {
        return projectileTrailTask;
    }

    public SkinsRestorerHook getSkinsRestorerHook() {
        return skinsRestorerHook;
    }

    public SkinChangePricing getSkinChangePricing() {
        return skinChangePricing;
    }
}
