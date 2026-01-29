package com.alexcreeper.bedwars1058.shop;

import com.alexcreeper.bedwars1058.api.configuration.Language;
import com.alexcreeper.bedwars1058.arena.Arena;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
// Removed SwordItem import
import net.minecraft.registry.Registries; // For ID Check
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopUtils {

    private static final Map<UUID, ShopCategory> openCategories = new HashMap<>();
    private static final Map<UUID, String> playerShops = new HashMap<>();
    private static final Map<UUID, Arena> playerArenas = new HashMap<>();

    public static void openShop(ServerPlayerEntity player, Arena arena) {
        String shopFile = (arena.getGroup() != null) ? arena.getGroup().getShopFileName() : "shop.yml";
        List<ShopCategory> categories = ShopManager.getShopForGroup(shopFile);
        
        playerShops.put(player.getUuid(), shopFile);
        playerArenas.put(player.getUuid(), arena);
        if (!categories.isEmpty()) openCategory(player, categories.get(0));
    }

    private static void openCategory(ServerPlayerEntity player, ShopCategory category) {
        openCategories.put(player.getUuid(), category);
        List<ShopCategory> allCats = ShopManager.getShopForGroup(playerShops.get(player.getUuid()));
        
        SimpleInventory inv = new SimpleInventory(54); 
        for (ShopCategory cat : allCats) {
            if (cat.getSlot() >= 0 && cat.getSlot() < 9) inv.setStack(cat.getSlot(), cat.getIcon().copy());
        }
        
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(""));
        for (int i = 9; i < 18; i++) inv.setStack(i, glass);
        
        for (ShopContent item : category.getContent()) {
            if (item.getSlot() >= 18 && item.getSlot() < 54) inv.setStack(item.getSlot(), formatItem(item));
        }

        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() { return Text.literal("Item Shop"); }
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity p) {
                return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
                    @Override
                    public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity playerEntity) {
                        if (slotId >= 0 && slotId < 54) {
                            if (actionType == SlotActionType.PICKUP) handleShopClick((ServerPlayerEntity) playerEntity, slotId);
                            return; 
                        }
                        super.onSlotClick(slotId, button, actionType, playerEntity);
                    }
                    @Override public boolean canUse(PlayerEntity player) { return true; }
                    @Override
                    public void onClosed(PlayerEntity player) {
                        super.onClosed(player);
                        openCategories.remove(player.getUuid());
                        playerShops.remove(player.getUuid());
                        playerArenas.remove(player.getUuid());
                    }
                };
            }
        });
    }

    private static void handleShopClick(ServerPlayerEntity player, int slotId) {
        List<ShopCategory> allCats = ShopManager.getShopForGroup(playerShops.get(player.getUuid()));
        
        if (slotId >= 0 && slotId < 9) {
            for (ShopCategory cat : allCats) {
                if (cat.getSlot() == slotId) { openCategory(player, cat); return; }
            }
        }
        
        if (slotId >= 18) {
            ShopCategory current = openCategories.get(player.getUuid());
            if (current != null) {
                for (ShopContent item : current.getContent()) {
                    if (item.getSlot() == slotId) { buyItem(player, item); return; }
                }
            }
        }
    }

    private static void buyItem(ServerPlayerEntity player, ShopContent item) {
        int balance = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item.getCurrency()) balance += stack.getCount();
        }
        
        if (balance >= item.getCost()) {
            int leftToRemove = item.getCost();
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == item.getCurrency()) {
                    int take = Math.min(stack.getCount(), leftToRemove);
                    stack.decrement(take);
                    leftToRemove -= take;
                    if (leftToRemove <= 0) break;
                }
            }

            ItemStack stack = item.getStack().copy();
            Item type = stack.getItem();
            Arena arena = playerArenas.get(player.getUuid());

            // 1. ARMOR
            if (arena != null && (type == Items.CHAINMAIL_BOOTS || type == Items.IRON_BOOTS || type == Items.DIAMOND_BOOTS)) {
                int tier = 0;
                if (type == Items.CHAINMAIL_BOOTS) tier = 1;
                if (type == Items.IRON_BOOTS) tier = 2;
                if (type == Items.DIAMOND_BOOTS) tier = 3;

                arena.setArmorTier(player.getUuid(), tier);
                arena.equipTeamArmor(player);
                
                player.sendMessage(Language.getMsg("ingame.shop.purchased").copy().append(" Armor"), true);
            } 
            // 2. SWORD (Safe Registry ID Check)
            else if (Registries.ITEM.getId(type).getPath().contains("_sword")) {
                 for (int i=0; i < player.getInventory().size(); i++) {
                     ItemStack s = player.getInventory().getStack(i);
                     if (Registries.ITEM.getId(s.getItem()).getPath().contains("_sword")) {
                         player.getInventory().setStack(i, ItemStack.EMPTY);
                     }
                 }
                 player.getInventory().offerOrDrop(stack);
                 player.sendMessage(Language.getMsg("ingame.shop.purchased").copy().append(" " + item.getStack().getName().getString()), true);
            } 
            // 3. NORMAL
            else {
                player.getInventory().offerOrDrop(stack);
                player.sendMessage(Language.getMsg("ingame.shop.purchased").copy().append(" " + item.getStack().getName().getString()), true);
            }
        } else {
            String currName = item.getCurrency().getName().getString();
            player.sendMessage(Language.getMsg("ingame.shop.not-enough-money").copy().append(" " + currName), true);
        }
    }

    private static ItemStack formatItem(ShopContent item) {
        ItemStack stack = item.getStack().copy();
        String currencyName = item.getCurrency().getName().getString();
        
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(stack.getItem().getName().getString()).formatted(Formatting.GREEN));
        
        Text costLine = Text.literal("Cost: " + item.getCost() + " " + currencyName).formatted(Formatting.GRAY);
        stack.set(DataComponentTypes.LORE, new LoreComponent(Collections.singletonList(costLine)));
        
        return stack;
    }
}