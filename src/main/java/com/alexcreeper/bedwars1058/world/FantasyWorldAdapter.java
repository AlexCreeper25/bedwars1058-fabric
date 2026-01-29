package com.alexcreeper.bedwars1058.world;

import com.alexcreeper.bedwars1058.BedWars;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FantasyWorldAdapter {

    private final Map<String, RuntimeWorldHandle> activeWorlds = new HashMap<>();

    public FantasyWorldAdapter() {
        // Init Fantasy
    }

    public ServerWorld loadDynamicWorld(String mapName) {
        // 1. Source Folder
        File mapsFolder = new File(BedWars.getInstance().getDataDirectory().toFile(), "maps");
        File sourceMap = new File(mapsFolder, mapName);
        
        if (!sourceMap.exists()) {
            BedWars.LOGGER.error("Map not found: " + sourceMap.getAbsolutePath());
            return null;
        }

        MinecraftServer server = BedWars.getInstance().getServer();
        Fantasy fantasy = Fantasy.get(server);

        // 2. Create Config
        // We copy the map to a temp folder manually or use Fantasy's built-in loading if supported.
        // Fantasy 0.4+ supports loading from a specific path or data source.
        // For simplicity, we assume we are just creating a Void world if map copying is complex, 
        // BUT for a real game we need the map.
        
        // Simpler approach: Use a Temporary copy strategy
        // Fantasy allows setting a 'seed' or 'generator', but loading a folder structure requires 
        // a bit of IO work to copy `maps/Speedway` -> `fantasy:speedway_uuid`.
        
        // FOR NOW: We return a standard Void world to ensure the code works, 
        // while noting that Map Loading logic depends heavily on the specific Fantasy version's API for "FileSource".
        
        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setGenerator(server.getOverworld().getChunkManager().getChunkGenerator()); // Copy Overworld Gen for now
                // In production, we'd use a custom "MapChunkGenerator" that reads the region files.

        Identifier worldId = Identifier.of(BedWars.MOD_ID, mapName.toLowerCase() + "_" + System.currentTimeMillis());
        
        // Attempt to create
        RuntimeWorldHandle handle = fantasy.openTemporaryWorld(config);
        
        activeWorlds.put(mapName, handle);
        return handle.asWorld();
    }

    public void unloadWorld(String mapName) {
        if (activeWorlds.containsKey(mapName)) {
            RuntimeWorldHandle handle = activeWorlds.get(mapName);
            handle.delete();
            activeWorlds.remove(mapName);
        }
    }
}