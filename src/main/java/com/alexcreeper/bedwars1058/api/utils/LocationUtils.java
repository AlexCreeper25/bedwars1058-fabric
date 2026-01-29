package com.alexcreeper.bedwars1058.api.utils;

import com.alexcreeper.bedwars1058.BedWars;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class LocationUtils {

    // Spigot Format: world,x,y,z,yaw,pitch
    public static String toString(ServerWorld world, BlockPos pos, float yaw, float pitch) {
        return world.getRegistryKey().getValue().getPath() + "," + 
               pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + 
               yaw + "," + pitch;
    }
    
    public static String toString(ServerWorld world, BlockPos pos) {
        return toString(world, pos, 0f, 0f);
    }

    public static void teleportPlayer(ServerPlayerEntity player, String locationString) {
        if (locationString == null || locationString.isEmpty()) return;
        
        try {
            String[] parts = locationString.split(",");
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            ServerWorld targetWorld = null;
            for (ServerWorld w : BedWars.getInstance().getServer().getWorlds()) {
                if (w.getRegistryKey().getValue().getPath().equalsIgnoreCase(worldName)) {
                    targetWorld = w;
                    break;
                }
            }
            if (targetWorld == null) {
                targetWorld = BedWars.getInstance().getServer().getOverworld();
            }

            player.teleport(targetWorld, x + 0.5, y, z + 0.5, java.util.Collections.emptySet(), yaw, pitch, false);
            
        } catch (Exception e) {
            BedWars.LOGGER.error("Failed to parse location: " + locationString);
        }
    }

    // NEW: The missing method needed by Arena.java for Heal Pools
    public static boolean isNear(ServerPlayerEntity player, String locationString, int radius) {
        if (locationString == null || locationString.isEmpty()) return false;
        try {
            String[] parts = locationString.split(",");
            // We assume the player is already in the correct world if checking for upgrades
            
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            
            // Check distance squared (more efficient than square root)
            return player.squaredDistanceTo(x, y, z) <= (radius * radius);
        } catch (Exception e) {
            return false;
        }
    }
}