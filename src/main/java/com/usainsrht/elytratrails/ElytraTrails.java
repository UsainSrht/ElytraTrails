package com.usainsrht.elytratrails;

import com.usainsrht.elytratrails.command.ElytraCommand;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.economy.VaultHook;
import com.usainsrht.elytratrails.gui.TrailGUI;
import com.usainsrht.elytratrails.listener.GUIListener;
import com.usainsrht.elytratrails.listener.PlayerListener;
import com.usainsrht.elytratrails.trail.ParticleTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElytraTrails extends JavaPlugin {

    private static ElytraTrails instance;

    private TrailManager trailManager;
    private PlayerDataManager playerDataManager;
    private VaultHook vaultHook;
    private TrailGUI trailGUI;
    private ParticleTask particleTask;

    @Override
    public void onEnable() {
        instance = this;

        // ── Configuration ────────────────────────────────────
        trailManager = new TrailManager(this);
        playerDataManager = new PlayerDataManager(this);

        // ── Economy ──────────────────────────────────────────
        vaultHook = new VaultHook();
        if (vaultHook.isEnabled()) {
            getLogger().info("Vault economy hooked successfully.");
        } else {
            getLogger().info("Vault not found — trail purchasing disabled.");
        }

        // ── Particle task (runs every tick; emitters control their own intervals) ──
        particleTask = new ParticleTask(this, trailManager, playerDataManager);
        particleTask.runTaskTimer(this, 0L, 1L);

        // ── GUI ──────────────────────────────────────────────
        trailGUI = new TrailGUI(this, trailManager, playerDataManager, vaultHook);

        // ── Listeners ────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new GUIListener(trailGUI), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(playerDataManager), this);

        // ── Commands ─────────────────────────────────────────
        ElytraCommand elytraCommand = new ElytraCommand(this, trailManager, playerDataManager, trailGUI);
        PluginCommand cmd = getCommand("elytra");
        if (cmd != null) {
            cmd.setExecutor(elytraCommand);
            cmd.setTabCompleter(elytraCommand);
        }

        getLogger().info("ElytraTrails v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("ElytraTrails disabled.");
    }

    // ── Accessors ────────────────────────────────────────────

    public static ElytraTrails getInstance() {
        return instance;
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

    public TrailGUI getTrailGUI() {
        return trailGUI;
    }
}
