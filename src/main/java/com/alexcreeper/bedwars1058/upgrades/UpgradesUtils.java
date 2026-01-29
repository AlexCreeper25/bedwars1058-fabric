package com.alexcreeper.bedwars1058.upgrades;

import com.alexcreeper.bedwars1058.api.configuration.Language;
import com.alexcreeper.bedwars1058.arena.Arena;
import com.alexcreeper.bedwars1058.arena.Team;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpgradesUtils {

    // Store which arena/team player is looking at
    private static final Map<UUID, Team> openTeams = new HashMap<>();

    public static void openUpgrades(ServerPlayerEntity player, Arena arena) {
        Team team = arena.getTeam(player.getUuid());
        if (team == null) return;
        
        openTeams.put(player.getUuid(), team);
        
        SimpleInventory inv = new SimpleInventory(27);
        
        for (UpgradeManager.TierUpgrade up : UpgradeManager.getUpgrades()) {
            if (up.slot >= 0 && up.slot < 27) {
                int currentLvl = team.getUpgradeLevel(up.key);
                
                // Determine Next Tier
                if (currentLvl < up.tiers.size()) {
                    UpgradeManager.TierUpgrade.Tier nextTier = up.tiers.get(currentLvl);
                    inv.setStack(up.slot, formatItem(nextTier, currentLvl + 1));
                } else {
                    // Maxed Out
                    ItemStack maxed = new ItemStack(Items.BARRIER);
                    maxed.set(DataComponentTypes.CUSTOM_NAME, Text.literal("MAXED").formatted(Formatting.RED));
                    inv.setStack(up.slot, maxed);
                }
            }
        }

        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() { return Text.literal("Team Upgrades"); }
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity p) {
                return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3) {
                    @Override
                    public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity playerEntity) {
                        if (slotId >= 0 && slotId < 27) {
                            if (actionType == SlotActionType.PICKUP) handleUpgradeClick((ServerPlayerEntity) playerEntity, slotId);
                            return; 
                        }
                        super.onSlotClick(slotId, button, actionType, playerEntity);
                    }
                    @Override public boolean canUse(PlayerEntity player) { return true; }
                    @Override public void onClosed(PlayerEntity player) { super.onClosed(player); openTeams.remove(player.getUuid()); }
                };
            }
        });
    }

    private static void handleUpgradeClick(ServerPlayerEntity player, int slotId) {
        Team team = openTeams.get(player.getUuid());
        if (team == null) return;
        
        for (UpgradeManager.TierUpgrade up : UpgradeManager.getUpgrades()) {
            if (up.slot == slotId) {
                int currentLvl = team.getUpgradeLevel(up.key);
                if (currentLvl >= up.tiers.size()) {
                    player.sendMessage(Text.literal("Already Maxed!").formatted(Formatting.RED), true);
                    return;
                }
                
                UpgradeManager.TierUpgrade.Tier next = up.tiers.get(currentLvl);
                
                // Buy Logic
                if (consumeCurrency(player, next.currency, next.cost)) {
                    team.setUpgradeLevel(up.key, currentLvl + 1);
                    player.sendMessage(Language.getMsg("ingame.shop.purchased").copy().append(" " + next.name), true);
                    
                    // Re-open/Refresh GUI to show next tier
                    // Simple refresh: close and re-open (or update inventory directly if we stored the inv)
                    player.closeHandledScreen(); 
                    // Note: Re-opening immediately in Fabric might need a tick delay or just work, 
                    // for Phase 1 we just close to confirm purchase.
                } else {
                    player.sendMessage(Language.getMsg("ingame.shop.not-enough-money").copy().append(" Diamond"), true);
                }
                return;
            }
        }
    }
    
    private static boolean consumeCurrency(ServerPlayerEntity player, net.minecraft.item.Item currency, int amount) {
        int balance = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == currency) balance += stack.getCount();
        }
        
        if (balance >= amount) {
            int left = amount;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == currency) {
                    int take = Math.min(stack.getCount(), left);
                    stack.decrement(take);
                    left -= take;
                    if (left <= 0) break;
                }
            }
            return true;
        }
        return false;
    }

    private static ItemStack formatItem(UpgradeManager.TierUpgrade.Tier tier, int lvl) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK); // Use specific items based on config in full version
        // Mimic BedWars1058 Lore
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(tier.name.replace("&", "\u00a7")).formatted(Formatting.YELLOW));
        return stack;
    }
}