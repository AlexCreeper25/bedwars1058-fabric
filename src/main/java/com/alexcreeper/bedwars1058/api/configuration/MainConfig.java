package com.alexcreeper.bedwars1058.api.configuration;

import com.alexcreeper.bedwars1058.BedWars;

public class MainConfig {
    
    private static ConfigManager config;
    
    public static void init() {
        config = new ConfigManager("config.yml", BedWars.getInstance().getDataDirectory());
        
        // Set defaults if file is new/empty
        if (config.getString("general.server-name") == null) {
            setDefaults();
        }
    }
    
    private static void setDefaults() {
        // General
        config.set("general.server-name", "BedWars Server");
        config.set("general.server-ip", "mc.example.com");
        
        // Game Settings
        config.set("game-settings.respawn-time", 5);
        config.set("game-settings.void-kill-height", 0);
        config.set("game-settings.allow-spectator-teleport", true);
        config.set("game-settings.enable-respawn-title", true);
        config.set("game-settings.player-drop-items", true);
        config.set("game-settings.spawn-protection-radius", 10);
        
        // Performance
        config.set("performance.async-arena-load", false);
        config.set("performance.world-reset-method", "fantasy");
        
        // Database
        config.set("database.enable", false);
        config.set("database.type", "sqlite");
        config.set("database.sqlite-file", "bedwars.db");
        config.set("database.mysql.host", "localhost");
        config.set("database.mysql.port", 3306);
        config.set("database.mysql.database", "bedwars");
        config.set("database.mysql.username", "root");
        config.set("database.mysql.password", "password");
        
        // Scoreboard
        config.set("scoreboard.update-interval", 20);
        
        config.save();
        BedWars.LOGGER.info("Created default config.yml");
    }
    
    // Getters for easy access
    public static int getRespawnTime() {
        return config.getInt("game-settings.respawn-time");
    }
    
    public static int getVoidKillHeight() {
        return config.getInt("game-settings.void-kill-height");
    }
    
    public static boolean isSpectatorTeleportEnabled() {
        return config.getBoolean("game-settings.allow-spectator-teleport");
    }
    
    public static boolean isRespawnTitleEnabled() {
        return config.getBoolean("game-settings.enable-respawn-title");
    }
    
    public static boolean isPlayerDropItemsEnabled() {
        return config.getBoolean("game-settings.player-drop-items");
    }
    
    public static int getSpawnProtectionRadius() {
        return config.getInt("game-settings.spawn-protection-radius");
    }
    
    public static boolean isDatabaseEnabled() {
        return config.getBoolean("database.enable");
    }
    
    public static String getDatabaseType() {
        return config.getString("database.type");
    }
    
    public static int getScoreboardUpdateInterval() {
        return config.getInt("scoreboard.update-interval");
    }
    
    public static void reload() {
        config.reload();
    }
    
    public static ConfigManager getConfig() {
        return config;
    }
}