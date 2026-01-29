package com.alexcreeper.bedwars1058.listener;

import com.alexcreeper.bedwars1058.BedWars;
import com.alexcreeper.bedwars1058.arena.Arena;
import com.alexcreeper.bedwars1058.arena.ArenaStatus;
import com.alexcreeper.bedwars1058.arena.Team;
import com.alexcreeper.bedwars1058.shop.ShopUtils;
import com.alexcreeper.bedwars1058.upgrades.UpgradesUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback; // NEW
import net.minecraft.block.BedBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity; // NEW
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity; // NEW
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import me.lucko.fabric.api.permissions.v0.Permissions;

import java.util.Optional;

public class GameListener {

    public static void register() {
        
        // 1. Block Break (Map Protection)
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity)) return true; 
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            Optional<Arena> arenaOpt = getArenaByPlayer(serverPlayer);
            if (!arenaOpt.isPresent()) return true; 
            Arena arena = arenaOpt.get();
            if (serverPlayer.isCreative() && Permissions.check(serverPlayer, "bedwars.admin")) return true;

            if (arena.getStatus() == ArenaStatus.PLAYING) {
                // Bed Breaking
                if (state.getBlock() instanceof BedBlock) {
                    Team myTeam = arena.getTeam(serverPlayer.getUuid());
                    for (Team targetTeam : arena.getTeams()) {
                        if (targetTeam.isBed(pos)) {
                            if (targetTeam == myTeam) {
                                serverPlayer.sendMessage(Text.literal("You cannot destroy your own bed!").formatted(Formatting.RED), true);
                                return false; 
                            }
                            if (targetTeam.isBedDestroyed()) return true; 
                            arena.breakBed(targetTeam, serverPlayer.getName().getString());
                            return true; 
                        }
                    }
                    return true; 
                }
                // Allow breaking player-placed blocks
                if (arena.isBlockPlayerPlaced(pos)) { 
                    arena.removePlacedBlock(pos); 
                    return true; 
                }
                serverPlayer.sendMessage(Text.literal("You can only break blocks placed by players!").formatted(Formatting.RED), true);
                return false; 
            }
            return false; // Cancel everything in Lobby/Spectator
        });

        // 2. Block Place (Tracking & TNT)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity)) return ActionResult.PASS;
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            
            ItemStack stack = player.getStackInHand(hand);
            Optional<Arena> arenaOpt = getArenaByPlayer(serverPlayer);
            if (!arenaOpt.isPresent()) return ActionResult.PASS;
            Arena arena = arenaOpt.get();

            if (arena.getStatus() == ArenaStatus.PLAYING) {
                // --- TNT AUTO-IGNITE LOGIC ---
                if (stack.getItem() == Items.TNT) {
                    BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
                    
                    TntEntity tnt = new TntEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, serverPlayer);
                    tnt.setFuse(80); // 4 Seconds fuse
                    world.spawnEntity(tnt);
                    world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    return ActionResult.SUCCESS; // Cancel block placement, handled manually
                }
                
                // Normal Block Placement
                if (stack.getItem() instanceof BlockItem) {
                    BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
                    arena.addPlacedBlock(placePos);
                }
                return ActionResult.PASS; 
            }
            
            // Lobby/Spectator Protection
            if (Permissions.check(player, "bedwars.admin") && player.isCreative()) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // 3. Fireball Launching
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            ItemStack stack = player.getStackInHand(hand);
            
            if (stack.getItem() == Items.FIRE_CHARGE) {
                Optional<Arena> arenaOpt = getArenaByPlayer((ServerPlayerEntity) player);
                if (arenaOpt.isPresent() && arenaOpt.get().getStatus() == ArenaStatus.PLAYING) {
                    
                    // Launch Ghast Fireball
                    Vec3d look = player.getRotationVec(1.0F);
                    FireballEntity fireball = new FireballEntity(world, player, look.x, look.y, look.z, 1);
                    fireball.setPosition(player.getX() + look.x, player.getEyeY() + look.y, player.getZ() + look.z);
                    fireball.powerX = look.x * 0.1;
                    fireball.powerY = look.y * 0.1;
                    fireball.powerZ = look.z * 0.1;
                    world.spawnEntity(fireball);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    
                    if (!player.isCreative()) stack.decrement(1);
                    return TypedActionResult.success(stack);
                }
            }
            return TypedActionResult.pass(stack);
        });

        // 4. PVP/Damage
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity)) return ActionResult.PASS;
            Optional<Arena> arenaOpt = getArenaByPlayer((ServerPlayerEntity) player);
            if (arenaOpt.isPresent() && arenaOpt.get().getStatus() != ArenaStatus.PLAYING) return ActionResult.FAIL; 
            return ActionResult.PASS;
        });
        
        // 5. Death Handling
        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
             Optional<Arena> arenaOpt = getArenaByPlayer(player);
             if (arenaOpt.isPresent() && arenaOpt.get().getStatus() == ArenaStatus.PLAYING) {
                 arenaOpt.get().handleDeath(player);
                 return false; 
             }
             return true; 
        });

        // 6. NPC Interactions (Shop/Upgrades)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (entity instanceof VillagerEntity) {
                VillagerEntity villager = (VillagerEntity) entity;
                if (!villager.hasCustomName()) return ActionResult.PASS;
                
                String name = villager.getCustomName().getString();
                Optional<Arena> arenaOpt = getArenaByPlayer((ServerPlayerEntity) player);
                
                if (arenaOpt.isPresent() && arenaOpt.get().getStatus() == ArenaStatus.PLAYING) {
                    if (name.contains("ITEM SHOP")) {
                        ShopUtils.openShop((ServerPlayerEntity) player, arenaOpt.get());
                        return ActionResult.SUCCESS;
                    } else if (name.contains("TEAM UPGRADES")) {
                        UpgradesUtils.openUpgrades((ServerPlayerEntity) player, arenaOpt.get());
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static Optional<Arena> getArenaByPlayer(ServerPlayerEntity player) {
        return BedWars.getInstance().getArenaManager().getArenas().stream().filter(a -> a.getPlayers().contains(player.getUuid()) || a.getSpectators().contains(player.getUuid())).findFirst();
    }
}