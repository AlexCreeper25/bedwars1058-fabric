package com.alexcreeper.bedwars1058;

import com.alexcreeper.bedwars1058.arena.ArenaManager;
import com.alexcreeper.bedwars1058.arena.GroupManager;
import com.alexcreeper.bedwars1058.command.BedWarsCommand;
import com.alexcreeper.bedwars1058.listener.GameListener;
import com.alexcreeper.bedwars1058.world.FantasyWorldAdapter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class BedWars implements ModInitializer {

    public static final String MOD_ID = "bedwars1058";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static BedWars instance;

    private FantasyWorldAdapter worldAdapter;
    private ArenaManager arenaManager;
    private GroupManager groupManager;
    private Path dataDirectory;

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("Initializing BedWars1058 Fabric...");
        
        // 1. Setup Data Folder
        this.dataDirectory = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        if (!dataDirectory.toFile().exists()) {
            dataDirectory.toFile().mkdirs();
        }

        // 2. Load Main Config FIRST
        com.alexcreeper.bedwars1058.api.configuration.MainConfig.init();
        
        // 3. Load Other Configuration Managers
        com.alexcreeper.bedwars1058.api.configuration.Language.init();
        com.alexcreeper.bedwars1058.shop.ShopManager.init();
        com.alexcreeper.bedwars1058.upgrades.UpgradeManager.init();
        com.alexcreeper.bedwars1058.sidebar.ScoreboardManager.init();

        // 3. Load Systems
        this.groupManager = new GroupManager();
        this.groupManager.loadGroups();

        this.worldAdapter = new FantasyWorldAdapter();
        this.arenaManager = new ArenaManager();
        this.arenaManager.loadArenas();
        
        // 4. Register Game Logic
        BedWarsCommand.register();
        GameListener.register();
    }

    public static BedWars getInstance() { return instance; }
    public net.minecraft.server.MinecraftServer getServer() { return (net.minecraft.server.MinecraftServer) FabricLoader.getInstance().getGameInstance(); }
    public FantasyWorldAdapter getWorldAdapter() { return worldAdapter; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public GroupManager getGroupManager() { return groupManager; }
    public Path getDataDirectory() { return dataDirectory; }
}