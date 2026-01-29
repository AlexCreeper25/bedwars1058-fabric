package com.alexcreeper.bedwars1058.api.utils;

import com.alexcreeper.bedwars1058.BedWars;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.Difficulty;
// import net.minecraft.world.GameRules; // Removed due to mapping conflict
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.File;
import java.nio.file.Path;

public class WorldAdapter {

    private final MinecraftServer server;

    public WorldAdapter(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Copies the map template and loads it as a persistent Fantasy world.
     */
    public ServerWorld loadDynamicWorld(String mapName) {
        BedWars.LOGGER.info("Loading dynamic world via Fantasy: " + mapName);
        
        // 1. Define the Dimension ID (e.g., bedwars1058:speedway)
        Identifier dimId = Identifier.of(BedWars.MOD_ID, mapName.toLowerCase());
        
        // 2. Prepare Source and Target Paths
        Path mapsFolder = BedWars.getInstance().getDataDirectory().resolve("maps");
        File sourceWorld = new File(mapsFolder.toFile(), mapName);
        
        Path sessionPath = server.getSavePath(WorldSavePath.ROOT);
        File targetDimFolder = sessionPath.resolve("dimensions").resolve(BedWars.MOD_ID).resolve(mapName.toLowerCase()).toFile();

        // 3. Validation
        if (!sourceWorld.exists()) {
            BedWars.LOGGER.error("Map template not found: " + sourceWorld.getAbsolutePath());
            return null;
        }

        // 4. Copy World Files
        try {
            if (targetDimFolder.exists()) {
                WorldUtils.deleteDirectory(targetDimFolder.toPath());
            }
            // Copy template to the dimension folder
            WorldUtils.copyDirectory(sourceWorld.toPath(), targetDimFolder.toPath());
            
            // Delete uid.dat to ensure a clean slate
            File uidFile = new File(targetDimFolder, "uid.dat");
            if (uidFile.exists()) uidFile.delete();
            
        } catch (Exception e) {
            BedWars.LOGGER.error("Failed to copy world files", e);
            return null;
        }

        // 5. Configure Fantasy
        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.NORMAL)
                // Removed GameRules setters to avoid compilation errors
                .setGenerator(server.getOverworld().getChunkManager().getChunkGenerator());

        // 6. Initialize the World
        Fantasy fantasy = Fantasy.get(server);
        
        // getOrOpenPersistentWorld looks for data in the dimensions folder matching the ID.
        RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(dimId, config);
        
        return handle.asWorld();
    }
    
    public void unloadWorld(String mapName) {
        BedWars.LOGGER.info("Unload requested for: " + mapName);
        // Fantasy persistent worlds persist until server restart or manual file deletion
    }
}