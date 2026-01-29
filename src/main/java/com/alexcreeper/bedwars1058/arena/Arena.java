package com.alexcreeper.bedwars1058.arena;

import com.alexcreeper.bedwars1058.BedWars;
import com.alexcreeper.bedwars1058.api.configuration.Language;
import com.alexcreeper.bedwars1058.api.configuration.MainConfig;
import com.alexcreeper.bedwars1058.api.utils.InventoryManager;
import com.alexcreeper.bedwars1058.api.utils.LocationUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Arena {

    private static final SoundEvent SOUND_NOTE_PLING = SoundEvent.of(Identifier.of("minecraft", "block.note_block.pling"));
    private static final SoundEvent SOUND_DRAGON_GROWL = SoundEvent.of(Identifier.of("minecraft", "entity.ender_dragon.growl"));
    private static final SoundEvent SOUND_WITHER_DEATH = SoundEvent.of(Identifier.of("minecraft", "entity.wither.death"));

    private final String arenaName;
    private final String worldName;
    private ServerWorld world;
    private ArenaStatus status;
    private Group group;
    
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();
    private final List<Generator> generators = new ArrayList<>();
    
    private final List<UUID> shopkeepers = new ArrayList<>();
    private final Set<BlockPos> placedBlocks = new HashSet<>();
    private final Map<UUID, Integer> armorTiers = new HashMap<>(); 
    
    private String lobbySpawnLocation;
    private String spectatorSpawnLocation;
    private int maxPlayers;
    private int minPlayers;
    private int groupSize;
    
    private int countdown;
    private static final int DEFAULT_COUNTDOWN = 20;

    public Arena(String name, String worldName) {
        this.arenaName = name;
        this.worldName = worldName;
        this.status = ArenaStatus.WAITING;
        this.countdown = DEFAULT_COUNTDOWN;
    }

    public void tick() {
        if (this.status == ArenaStatus.WAITING) {
            if (players.size() >= minPlayers) {
                this.status = ArenaStatus.STARTING;
                this.countdown = DEFAULT_COUNTDOWN;
                broadcast(Language.getMsg("ingame.game-start-countdown").copy().append(" " + countdown));
            }
        } else if (this.status == ArenaStatus.STARTING) {
            if (players.size() < minPlayers) {
                this.status = ArenaStatus.WAITING;
                this.countdown = DEFAULT_COUNTDOWN;
                broadcast(Language.getMsg("ingame.not-enough-players"));
                return;
            }
            if (BedWars.getInstance().getServer().getTicks() % 20 == 0) doCountdown();
            
        } else if (this.status == ArenaStatus.PLAYING) {
            if (world != null) {
                for (Generator gen : generators) {
                    int forgeLevel = 0;
                    for (Team t : teams) {
                        if (gen.isTeamGenerator(t)) {
                            forgeLevel = t.getUpgradeLevel("forge");
                            break;
                        }
                    }
                    gen.tick(world, forgeLevel);
                }
                
                if (BedWars.getInstance().getServer().getTicks() % 20 == 0) {
                    applyTeamEffects();
                    checkVoid();
                    checkWinner();
                }
            }
        }
        
        if (BedWars.getInstance().getServer().getTicks() % 20 == 0) {
            for (UUID uuid : players) {
                ServerPlayerEntity p = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
                if (p != null) com.alexcreeper.bedwars1058.sidebar.ScoreboardManager.updateScoreboard(p, this);
            }
        }
    }

    private void applyTeamEffects() {
        for (Team team : teams) {
            int haste = team.getUpgradeLevel("haste");
            int heal = team.getUpgradeLevel("heal-pool");
            
            for (UUID uuid : team.getMembers()) {
                if (!players.contains(uuid) || spectators.contains(uuid)) continue;
                ServerPlayerEntity p = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
                if (p == null) continue;
                
                if (haste > 0) p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, haste - 1, true, false, false));
                
                if (heal > 0 && team.getSpawnLocation() != null) {
                    if (LocationUtils.isNear(p, team.getSpawnLocation(), 15)) {
                         p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, true, false, false));
                    }
                }
            }
        }
    }

    private void checkVoid() {
        int voidHeight = MainConfig.getVoidKillHeight();
        for (UUID uuid : new ArrayList<>(players)) {
            if (spectators.contains(uuid)) continue;
            ServerPlayerEntity p = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
            if (p != null && p.getY() < voidHeight) handleDeath(p);
        }
    }

    public void checkWinner() {
        if (status != ArenaStatus.PLAYING) return;
        List<Team> aliveTeams = new ArrayList<>();
        for (Team team : teams) {
            boolean hasLivingMembers = team.getMembers().stream().anyMatch(uuid -> players.contains(uuid) && !spectators.contains(uuid));
            if (hasLivingMembers) aliveTeams.add(team);
        }

        if (aliveTeams.size() == 1) {
            endGame(aliveTeams.get(0));
        } else if (aliveTeams.isEmpty() && !players.isEmpty()) {
            resetArena();
        }
    }

    private void endGame(Team winner) {
        this.status = ArenaStatus.WAITING; 
        broadcast(Language.getMsg("ingame.messages.win").copy().append(" " + winner.getName()));
        playSound(SOUND_DRAGON_GROWL, 1.0f, 1.0f);
        new Thread(() -> {
            try { Thread.sleep(5000); BedWars.getInstance().getServer().execute(this::resetArena); } catch (InterruptedException e) {}
        }).start();
    }
    
    private void resetArena() {
        // Remove NPCs first
        if (world != null) {
            for (UUID uuid : shopkeepers) {
                net.minecraft.entity.Entity e = world.getEntity(uuid);
                if (e != null) e.discard();
            }
            shopkeepers.clear();
        }
        
        // Clear tracking data
        placedBlocks.clear();
        armorTiers.clear();
        
        // Reset teams
        for(Team t : teams) {
            t.setBedDestroyed(false);
            t.getMembers().clear();
            // Reset upgrade levels
            t.setUpgradeLevel("forge", 0);
            t.setUpgradeLevel("haste", 0);
            t.setUpgradeLevel("heal-pool", 0);
            t.setUpgradeLevel("sharpness", 0);
            t.setUpgradeLevel("protection", 0);
        }

        // Teleport and restore players
        for (UUID uuid : new ArrayList<>(players)) {
            ServerPlayerEntity p = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
            if (p != null) {
                // Change to adventure mode and clear
                p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
                p.getInventory().clear();
                p.clearStatusEffects();
                
                // Restore original inventory
                InventoryManager.restoreInventory(p);
                
                // Teleport to spawn world
                ServerWorld overworld = BedWars.getInstance().getServer().getOverworld();
                BlockPos spawn = new BlockPos(0, 100, 0); 
                p.teleport(overworld, spawn.getX(), spawn.getY(), spawn.getZ(), Collections.emptySet(), 0f, 0f, false);
            }
        }
        
        // Clear player lists
        this.players.clear();
        this.spectators.clear();
        this.status = ArenaStatus.WAITING;
        
        // Reload world to reset map
        BedWars.getInstance().getWorldAdapter().unloadWorld(worldName);
        ServerWorld newWorld = BedWars.getInstance().getWorldAdapter().loadDynamicWorld(worldName);
        if (newWorld != null) this.world = newWorld;
        
        BedWars.LOGGER.info("Arena " + arenaName + " has been reset");
    }

    public void handleDeath(ServerPlayerEntity player) {
        if (spectators.contains(player.getUuid())) return;
        Team team = getTeam(player.getUuid());
        if (team == null) return;

        // Drop resources if configured
        if (MainConfig.isPlayerDropItemsEnabled()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                 ItemStack stack = player.getInventory().getStack(i);
                 if (stack.getItem() == Items.IRON_INGOT || stack.getItem() == Items.GOLD_INGOT 
                     || stack.getItem() == Items.DIAMOND || stack.getItem() == Items.EMERALD) {
                     player.dropItem(stack.copy(), true, false);
                 }
            }
        }

        // Clear player state
        player.getInventory().clear(); 
        player.setHealth(20f);
        player.clearStatusEffects();
        player.getHungerManager().setFoodLevel(20);
        
        // Check if bed is destroyed (final kill)
        if (team.isBedDestroyed()) {
            eliminatePlayer(player);
        } else {
            startRespawnTask(player, team);
        }
    }
    
    private void eliminatePlayer(ServerPlayerEntity player) {
        spectators.add(player.getUuid());
        player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        
        // Teleport to spectator spawn or high up
        if (spectatorSpawnLocation != null && !spectatorSpawnLocation.isEmpty()) {
            LocationUtils.teleportPlayer(player, spectatorSpawnLocation);
        } else if (world != null) {
            player.teleport(world, 0, 100, 0, Collections.emptySet(), 0f, 0f, false);
        }
        
        // Give spectator items
        giveSpectatorItems(player);
        
        broadcast(Language.getMsg("ingame.messages.eliminated").copy().append(" " + player.getName().getString()));
        checkWinner();
    }
    
    private void giveSpectatorItems(ServerPlayerEntity player) {
        player.getInventory().clear();
        
        // Give compass for teleporting (future implementation)
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponentTypes.CUSTOM_NAME, 
            Text.literal("Teleporter").formatted(Formatting.GREEN));
        player.getInventory().setStack(0, compass);
        
        // Give leave item
        ItemStack leave = new ItemStack(Items.RED_BED);
        leave.set(DataComponentTypes.CUSTOM_NAME, 
            Text.literal("Leave Arena").formatted(Formatting.RED));
        player.getInventory().setStack(8, leave);
    }
    
    private void startRespawnTask(ServerPlayerEntity player, Team team) {
        spectators.add(player.getUuid()); 
        player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        
        // Teleport up temporarily
        if (world != null) player.teleport(world, player.getX(), 100, player.getZ(), Collections.emptySet(), 0f, 0f, false);
        
        int respawnTime = MainConfig.getRespawnTime();
        player.sendMessage(Language.getMsg("ingame.messages.respawning").copy().append(" " + respawnTime), true);
        
        new Thread(() -> {
            try {
                for (int i = respawnTime; i > 0; i--) {
                    final int seconds = i;
                    BedWars.getInstance().getServer().execute(() -> {
                        if (players.contains(player.getUuid())) {
                            player.sendMessage(Language.getMsg("ingame.messages.respawning").copy().append(" " + seconds), true);
                        }
                    });
                    Thread.sleep(1000);
                }
                
                // Respawn player
                BedWars.getInstance().getServer().execute(() -> {
                    if (players.contains(player.getUuid()) && status == ArenaStatus.PLAYING) {
                        spectators.remove(player.getUuid());
                        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                        
                        // Teleport to team spawn
                        if (team.getSpawnLocation() != null) {
                            LocationUtils.teleportPlayer(player, team.getSpawnLocation());
                        }
                        
                        // Give starting items
                        player.getInventory().offerOrDrop(new ItemStack(Items.WOODEN_SWORD));
                        equipTeamArmor(player);

                        player.sendMessage(Language.getMsg("ingame.messages.respawned"), true);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void setArmorTier(UUID uuid, int tier) { armorTiers.put(uuid, tier); }
    public int getArmorTier(UUID uuid) { return armorTiers.getOrDefault(uuid, 0); }

    public void equipTeamArmor(ServerPlayerEntity player) {
        Team team = getTeam(player.getUuid());
        if (team == null) return;
        
        int tier = getArmorTier(player.getUuid());
        int colorInt = team.getColor().getColorValue() != null ? team.getColor().getColorValue() : 0xFFFFFF;

        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        ItemStack leggings = new ItemStack(Items.LEATHER_LEGGINGS);
        ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        
        if (tier >= 1) { boots = new ItemStack(Items.CHAINMAIL_BOOTS); leggings = new ItemStack(Items.CHAINMAIL_LEGGINGS); }
        if (tier >= 2) { boots = new ItemStack(Items.IRON_BOOTS); leggings = new ItemStack(Items.IRON_LEGGINGS); }
        if (tier >= 3) { boots = new ItemStack(Items.DIAMOND_BOOTS); leggings = new ItemStack(Items.DIAMOND_LEGGINGS); }

        if (boots.getItem() == Items.LEATHER_BOOTS) applyColor(boots, colorInt);
        if (leggings.getItem() == Items.LEATHER_LEGGINGS) applyColor(leggings, colorInt);
        applyColor(chest, colorInt);
        applyColor(helmet, colorInt);

        player.equipStack(EquipmentSlot.FEET, boots);
        player.equipStack(EquipmentSlot.LEGS, leggings);
        player.equipStack(EquipmentSlot.CHEST, chest);
        player.equipStack(EquipmentSlot.HEAD, helmet);
    }

    private void applyColor(ItemStack stack, int color) {
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color));
    }
    
    public void breakBed(Team victimTeam, String breakerName) {
        if (victimTeam.isBedDestroyed()) return;
        victimTeam.setBedDestroyed(true);
        playSound(SOUND_WITHER_DEATH, 1.0f, 1.0f);
        broadcast(Language.getMsg("ingame.messages.bed-destroyed").copy().append(" Team " + victimTeam.getName()).append(" " + breakerName));
    }

    private void doCountdown() {
        this.countdown--;
        if (countdown <= 5 || countdown == 10 || countdown == 20) {
            broadcast(Language.getMsg("ingame.game-start-countdown").copy().append(" " + countdown));
            playSound(SOUND_NOTE_PLING, 1.0f, 1.0f);
        }
        if (this.countdown <= 0) startGame();
    }

    private void startGame() {
        this.status = ArenaStatus.PLAYING;
        broadcast(Language.getMsg("ingame.game-start"));
        playSound(SOUND_DRAGON_GROWL, 1.0f, 1.0f);
        
        assignTeams();
        
        if (world != null) {
            for (Team team : teams) {
                if (team.getShopLocation() != null) {
                    try {
                        String[] parts = team.getShopLocation().split(",");
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        float yaw = Float.parseFloat(parts[4]);
                        
                        spawnNPC(x, y, z, yaw, "ITEM SHOP");
                        spawnNPC(x + 2, y, z, yaw - 15, "TEAM UPGRADES");
                    } catch (Exception e) {}
                }
            }
        }
        
        for (Team team : teams) {
            for (UUID uuid : team.getMembers()) {
                ServerPlayerEntity player = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
                if (player != null && team.getSpawnLocation() != null) {
                    LocationUtils.teleportPlayer(player, team.getSpawnLocation());
                    player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                    setArmorTier(player.getUuid(), 0);
                    equipTeamArmor(player);
                    player.getInventory().offerOrDrop(new ItemStack(Items.WOODEN_SWORD));
                }
            }
        }
    }
    
    private void spawnNPC(double x, double y, double z, float yaw, String name) {
        VillagerEntity villager = EntityType.VILLAGER.create(world, SpawnReason.COMMAND);
        if (villager != null) {
            villager.refreshPositionAndAngles(x, y, z, yaw, 0f);
            villager.setAiDisabled(true);
            villager.setInvulnerable(true);
            villager.setCustomName(Text.literal(name).formatted(Formatting.YELLOW));
            villager.setCustomNameVisible(true);
            world.spawnEntity(villager);
            shopkeepers.add(villager.getUuid());
        }
    }
    
    private void assignTeams() {
        List<UUID> shufflePlayers = new ArrayList<>(players);
        Collections.shuffle(shufflePlayers);
        int teamIndex = 0;
        int groupCount = 0;
        if (teams.isEmpty()) return;
        for (UUID uuid : shufflePlayers) {
            Team currentTeam = teams.get(teamIndex);
            currentTeam.addMember(uuid);
            groupCount++;
            if (groupCount >= groupSize) {
                teamIndex++;
                groupCount = 0;
                if (teamIndex >= teams.size()) teamIndex = 0; 
            }
        }
    }

    private void broadcast(Text message) {
        for (UUID uuid : players) {
            ServerPlayerEntity p = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
            if (p != null) p.sendMessage(message, false);
        }
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        for (UUID uuid : players) {
            ServerPlayerEntity p = BedWars.getInstance().getServer().getPlayerManager().getPlayer(uuid);
            if (p != null) p.playSound(sound, volume, pitch);
        }
    }
    
    public boolean addPlayer(ServerPlayerEntity player) {
        if (status == ArenaStatus.PLAYING || players.size() >= maxPlayers) return false;
        if (players.contains(player.getUuid())) return true;
        
        // Save player's inventory before joining
        InventoryManager.saveInventory(player);
        
        players.add(player.getUuid());
        broadcast(Text.literal(player.getName().getString() + " joined (" + players.size() + "/" + maxPlayers + ")").formatted(Formatting.GRAY));
        
        // Teleport to lobby
        if (this.lobbySpawnLocation != null && !this.lobbySpawnLocation.isEmpty()) {
            LocationUtils.teleportPlayer(player, lobbySpawnLocation);
        }
        
        // Set to adventure mode and clear inventory for lobby
        player.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
        player.getInventory().clear();
        
        return true;
    }

    public void removePlayer(ServerPlayerEntity player) {
        players.remove(player.getUuid());
        spectators.remove(player.getUuid());
        armorTiers.remove(player.getUuid());
        
        // Restore player's inventory
        InventoryManager.restoreInventory(player);
        
        // Reset gamemode to survival
        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
        
        broadcast(Text.literal(player.getName().getString() + " left.").formatted(Formatting.YELLOW));
        if (status == ArenaStatus.PLAYING) checkWinner();
    }
    
    public void addPlacedBlock(BlockPos pos) { placedBlocks.add(pos); }
    public void removePlacedBlock(BlockPos pos) { placedBlocks.remove(pos); }
    public boolean isBlockPlayerPlaced(BlockPos pos) { return placedBlocks.contains(pos); }

    public Team getTeam(UUID playerIds) { for(Team t : teams) { if(t.getMembers().contains(playerIds)) return t; } return null; }
    public void addGenerator(Generator generator) { this.generators.add(generator); }
    public List<Generator> getGenerators() { return new ArrayList<>(generators); }
    public void addTeam(Team team) { this.teams.add(team); }
    public void setLobbySpawnLocation(String loc) { this.lobbySpawnLocation = loc; }
    public String getLobbySpawnLocation() { return lobbySpawnLocation; }
    public void setSpectatorSpawnLocation(String loc) { this.spectatorSpawnLocation = loc; }
    public void setMaxPlayers(int max) { this.maxPlayers = max; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMinPlayers(int min) { this.minPlayers = min; }
    public int getMinPlayers() { return minPlayers; }
    public void setGroupSize(int size) { this.groupSize = size; }
    public String getName() { return arenaName; }
    public String getWorldName() { return worldName; }
    public ServerWorld getWorld() { return world; }
    public void setWorld(ServerWorld world) { this.world = world; }
    public ArenaStatus getStatus() { return status; }
    public void setStatus(ArenaStatus status) { this.status = status; }
    public List<UUID> getPlayers() { return new ArrayList<>(players); }
    public List<UUID> getSpectators() { return new ArrayList<>(spectators); }
    public List<Team> getTeams() { return new ArrayList<>(teams); }
    public int getCountdown() { return countdown; }
    public void setGroup(Group group) { this.group = group; }
    public Group getGroup() { return group; }
}