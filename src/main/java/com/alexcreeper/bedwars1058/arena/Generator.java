package com.alexcreeper.bedwars1058.arena;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

public class Generator {
    public enum Type { IRON, GOLD, DIAMOND, EMERALD }

    private final Type type;
    private final String location;
    private int timer;
    private final int spawnDelay;

    public Generator(Type type, String location) {
        this.type = type;
        this.location = location;
        this.spawnDelay = getDefaultDelay(type);
        this.timer = spawnDelay;
    }

    public void tick(ServerWorld world) {
        tick(world, 0);
    }
    
    public void tick(ServerWorld world, int forgeLevel) {
        int delay = spawnDelay;
        if (type == Type.IRON || type == Type.GOLD) {
            if (forgeLevel == 1) delay = (int)(spawnDelay * 0.75);
            else if (forgeLevel == 2) delay = (int)(spawnDelay * 0.60);
            else if (forgeLevel == 3) delay = (int)(spawnDelay * 0.50);
            else if (forgeLevel == 4) delay = (int)(spawnDelay * 0.35);
        }

        if (timer-- <= 0) {
            spawnItem(world);
            timer = delay;
        }
    }

    private void spawnItem(ServerWorld world) {
        if (location == null || location.isEmpty()) return;
        try {
            String[] parts = location.split(",");
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            ItemStack stack = new ItemStack(getItemType());
            ItemEntity item = new ItemEntity(world, x + 0.5, y, z + 0.5, stack);
            item.setVelocity(0, 0, 0);
            world.spawnEntity(item);
        } catch (Exception e) {}
    }

    private int getDefaultDelay(Type type) {
        switch (type) {
            case IRON: return 20; 
            case GOLD: return 140; 
            case DIAMOND: return 600; 
            case EMERALD: return 1200; 
            default: return 100;
        }
    }
    
    private net.minecraft.item.Item getItemType() {
        switch (type) {
            case IRON: return Items.IRON_INGOT;
            case GOLD: return Items.GOLD_INGOT;
            case DIAMOND: return Items.DIAMOND;
            case EMERALD: return Items.EMERALD;
            default: return Items.IRON_INGOT;
        }
    }
    
    public Type getType() { return type; }
    public String getLocationString() { return location; }
    
    public boolean isTeamGenerator(Team team) {
         if (type != Type.IRON && type != Type.GOLD) return false;
         if (team.getSpawnLocation() == null || location == null) return false;
         try {
             String[] genParts = location.split(",");
             String[] teamParts = team.getSpawnLocation().split(",");
             double gx = Double.parseDouble(genParts[1]);
             double gy = Double.parseDouble(genParts[2]);
             double gz = Double.parseDouble(genParts[3]);
             double tx = Double.parseDouble(teamParts[1]);
             double ty = Double.parseDouble(teamParts[2]);
             double tz = Double.parseDouble(teamParts[3]);
             
             double distSq = Math.pow(gx-tx, 2) + Math.pow(gy-ty, 2) + Math.pow(gz-tz, 2);
             return distSq < 225.0; // 15 blocks
         } catch (Exception e) { return false; }
    }
}