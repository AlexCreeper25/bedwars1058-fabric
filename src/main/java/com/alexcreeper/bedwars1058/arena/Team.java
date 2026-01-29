package com.alexcreeper.bedwars1058.arena;

import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Team {

    private final String name;
    private final Formatting color;
    private String spawnLocation;
    private String bedLocation;
    private String shopLocation;
    private boolean bedDestroyed;
    private final List<UUID> members = new ArrayList<>();
    
    // NEW: Upgrade Levels (e.g., "protection" -> 2)
    private final Map<String, Integer> upgrades = new HashMap<>();

    public Team(String name, Formatting color) {
        this.name = name;
        this.color = color;
        this.bedDestroyed = false;
    }

    public boolean isBed(BlockPos pos) {
        if (bedLocation == null || bedLocation.isEmpty()) return false;
        try {
            String[] parts = bedLocation.split(",");
            int bx = (int) Double.parseDouble(parts[1]);
            int by = (int) Double.parseDouble(parts[2]);
            int bz = (int) Double.parseDouble(parts[3]);
            return pos.getSquaredDistance(bx, by, bz) <= 4.0; 
        } catch (Exception e) {
            return false;
        }
    }

    public void addMember(UUID uuid) { if (!members.contains(uuid)) members.add(uuid); }
    public List<UUID> getMembers() { return new ArrayList<>(members); }
    public String getName() { return name; }
    public Formatting getColor() { return color; }
    
    public String getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(String spawnLocation) { this.spawnLocation = spawnLocation; }
    
    public String getBedLocation() { return bedLocation; }
    public void setBedLocation(String bedLocation) { this.bedLocation = bedLocation; }
    
    public String getShopLocation() { return shopLocation; }
    public void setShopLocation(String shopLocation) { this.shopLocation = shopLocation; }
    
    public boolean isBedDestroyed() { return bedDestroyed; }
    public void setBedDestroyed(boolean bedDestroyed) { this.bedDestroyed = bedDestroyed; }
    
    // NEW: Upgrade Methods
    public int getUpgradeLevel(String key) { return upgrades.getOrDefault(key, 0); }
    public void setUpgradeLevel(String key, int level) { upgrades.put(key, level); }
}