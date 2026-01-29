package com.alexcreeper.bedwars1058.api.utils;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {
    
    private static final Map<UUID, SavedInventory> savedInventories = new HashMap<>();
    
    public static class SavedInventory {
        ItemStack[] mainInventory;
        ItemStack[] armorInventory;
        ItemStack offHand;
        int selectedSlot;
        float health;
        int foodLevel;
        
        public SavedInventory(ServerPlayerEntity player) {
            PlayerInventory inv = player.getInventory();
            
            // Save main inventory
            mainInventory = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                mainInventory[i] = inv.getStack(i).copy();
            }
            
            // Save armor
            armorInventory = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                armorInventory[i] = player.getInventory().armor.get(i).copy();
            }
            
            // Save offhand
            offHand = player.getOffHandStack().copy();
            selectedSlot = inv.selectedSlot;
            
            // Save health and hunger
            health = player.getHealth();
            foodLevel = player.getHungerManager().getFoodLevel();
        }
        
        public void restore(ServerPlayerEntity player) {
            PlayerInventory inv = player.getInventory();
            inv.clear();
            
            // Restore main inventory
            for (int i = 0; i < 36; i++) {
                inv.setStack(i, mainInventory[i].copy());
            }
            
            // Restore armor
            for (int i = 0; i < 4; i++) {
                player.getInventory().armor.set(i, armorInventory[i].copy());
            }
            
            // Restore offhand
            player.setStackInHand(Hand.OFF_HAND, offHand.copy());
            inv.selectedSlot = selectedSlot;
            
            // Restore health and hunger
            player.setHealth(health);
            player.getHungerManager().setFoodLevel(foodLevel);
        }
    }
    
    public static void saveInventory(ServerPlayerEntity player) {
        savedInventories.put(player.getUuid(), new SavedInventory(player));
    }
    
    public static void restoreInventory(ServerPlayerEntity player) {
        SavedInventory saved = savedInventories.remove(player.getUuid());
        if (saved != null) {
            saved.restore(player);
        }
    }
    
    public static void clearSaved(UUID uuid) {
        savedInventories.remove(uuid);
    }
    
    public static boolean hasSaved(UUID uuid) {
        return savedInventories.containsKey(uuid);
    }
}