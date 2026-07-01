package com.usainsrht.elytratrails;

import com.usainsrht.elytratrails.command.ElytraCommand;
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
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElytraTrails extends JavaPlugin {

    private static ElytraTrails instance;

    private TrailManager trailManager;
    private PlayerDataManager playerDataManager;
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private CosmeticsGUI cosmeticsGUI;
    private TrailGUI trailGUI;
    private ParticleTask particleTask;
    private ProjectileTrailTask projectileTrailTask;

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

        // ── WorldGuard ───────────────────────────────────────
        worldGuardHook = new WorldGuardHook(getLogger());

        // ── GUI (build CosmeticsGUI after TrailGUI; resolve circular ref) ──
        // TrailGUI needs CosmeticsGUI for the Back button, so we construct
        // CosmeticsGUI first with a placeholder, then inject.
        // Solution: TrailGUI receives cosmeticsGUI as a field we set post-construction.
        trailGUI = new TrailGUI(this, trailManager, playerDataManager, vaultHook, null);
        cosmeticsGUI = new CosmeticsGUI(trailGUI);
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
        ElytraCommand elytraCommand = new ElytraCommand(
                this, trailManager, playerDataManager, cosmeticsGUI, trailGUI);
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
        if (projectileTrailTask != null) {
            projectileTrailTask.cancel();
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
}
