package com.alexcreeper.bedwars1058.command;

import com.alexcreeper.bedwars1058.BedWars;
import com.alexcreeper.bedwars1058.api.utils.LocationUtils;
import com.alexcreeper.bedwars1058.arena.Arena;
import com.alexcreeper.bedwars1058.arena.Generator;
import com.alexcreeper.bedwars1058.arena.Team;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import me.lucko.fabric.api.permissions.v0.Permissions;

import java.util.Optional;

import static net.minecraft.server.command.CommandManager.*;

public class BedWarsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            
            dispatcher.register(literal("bw")
                .requires(Permissions.require("bedwars.cmd", 0)) 
                
                // Join / List
                .then(literal("join").then(argument("arena", StringArgumentType.word()).executes(BedWarsCommand::executeJoin)))
                .then(literal("list").executes(BedWarsCommand::executeList))
                
                // Admin Setup
                .then(literal("setup").requires(Permissions.require("bedwars.admin", 2)).then(argument("arena", StringArgumentType.word()).then(argument("mapFolder", StringArgumentType.word()).executes(BedWarsCommand::executeSetup))))
                .then(literal("delete").requires(Permissions.require("bedwars.admin", 2)).then(argument("arena", StringArgumentType.word()).executes(BedWarsCommand::executeDelete)))
                .then(literal("setlobby").requires(Permissions.require("bedwars.admin", 2)).then(argument("arena", StringArgumentType.word()).executes(BedWarsCommand::executeSetLobby)))
                .then(literal("setgen").requires(Permissions.require("bedwars.admin", 2)).then(argument("arena", StringArgumentType.word()).then(argument("type", StringArgumentType.word()).executes(BedWarsCommand::executeSetGen))))

                // Team Commands
                .then(literal("team")
                    .requires(Permissions.require("bedwars.admin", 2))
                    .then(literal("add").then(argument("arena", StringArgumentType.word()).then(argument("teamName", StringArgumentType.word()).then(argument("color", StringArgumentType.word()).executes(BedWarsCommand::executeTeamAdd)))))
                    .then(literal("remove").then(argument("arena", StringArgumentType.word()).then(argument("teamName", StringArgumentType.word()).executes(BedWarsCommand::executeTeamRemove))))
                    .then(literal("spawn").then(argument("arena", StringArgumentType.word()).then(argument("teamName", StringArgumentType.word()).executes(BedWarsCommand::executeTeamSpawn))))
                    .then(literal("bed").then(argument("arena", StringArgumentType.word()).then(argument("teamName", StringArgumentType.word()).executes(BedWarsCommand::executeTeamBed))))
                    
                    // NEW: Shop Location
                    .then(literal("shop").then(argument("arena", StringArgumentType.word()).then(argument("teamName", StringArgumentType.word()).executes(BedWarsCommand::executeTeamShop))))
                )
            );
            
            dispatcher.register(literal("bedwars").redirect(dispatcher.getRoot().getChild("bw")));
        });
    }

    // --- NEW METHOD ---
    private static int executeTeamShop(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            String teamName = StringArgumentType.getString(context, "teamName");
            Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
            if (arenaOpt.isPresent()) {
                Optional<Team> t = arenaOpt.get().getTeams().stream().filter(te -> te.getName().equalsIgnoreCase(teamName)).findFirst();
                if (t.isPresent()) {
                    String loc = LocationUtils.toString((ServerWorld)player.getEntityWorld(), player.getBlockPos(), player.getYaw(), player.getPitch());
                    t.get().setShopLocation(loc); // Save shop loc
                    BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
                    context.getSource().sendMessage(Text.literal("Shop NPC location set for " + teamName).formatted(Formatting.GREEN));
                } else {
                    context.getSource().sendMessage(Text.literal("Team not found.").formatted(Formatting.RED));
                }
            }
        } catch (Exception e) {}
        return Command.SINGLE_SUCCESS;
    }

    // --- Helpers (Paste existing implementations) ---
    private static int executeSetup(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeSetupFull(c); }
    private static int executeDelete(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeDeleteFull(c); }
    private static int executeTeamAdd(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeTeamAddFull(c); }
    private static int executeTeamRemove(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeTeamRemoveFull(c); }
    private static int executeSetGen(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeSetGenFull(c); }
    private static int executeJoin(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeJoinFull(c); }
    private static int executeList(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeListFull(c); }
    private static int executeSetLobby(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeSetLobbyFull(c); }
    private static int executeTeamSpawn(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeTeamSpawnFull(c); }
    private static int executeTeamBed(CommandContext<ServerCommandSource> c) { return BedWarsCommand.executeTeamBedFull(c); }
    
    // --- FULL METHODS (Hidden for brevity, ensure previous logic is here) ---
    
    private static int executeSetupFull(CommandContext<ServerCommandSource> context) {
         try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            String mapFolder = StringArgumentType.getString(context, "mapFolder");
            context.getSource().sendMessage(Text.literal("Creating arena...").formatted(Formatting.YELLOW));
            if(BedWars.getInstance().getArenaManager().createArena(arenaName, mapFolder, player)) {
                context.getSource().sendMessage(Text.literal("Arena created!").formatted(Formatting.GREEN));
            } else {
                context.getSource().sendMessage(Text.literal("Failed. Map folder missing?").formatted(Formatting.RED));
            }
        } catch (Exception e) {} return Command.SINGLE_SUCCESS;
    }
    
    private static int executeDeleteFull(CommandContext<ServerCommandSource> context) {
        String arenaName = StringArgumentType.getString(context, "arena");
        BedWars.getInstance().getArenaManager().deleteArena(arenaName);
        context.getSource().sendMessage(Text.literal("Deleted " + arenaName).formatted(Formatting.RED));
        return Command.SINGLE_SUCCESS;
    }
    
    private static int executeSetLobbyFull(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
            if (arenaOpt.isPresent()) {
                String loc = LocationUtils.toString((ServerWorld)player.getEntityWorld(), player.getBlockPos(), player.getYaw(), player.getPitch());
                arenaOpt.get().setLobbySpawnLocation(loc);
                BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
                context.getSource().sendMessage(Text.literal("Lobby updated.").formatted(Formatting.GREEN));
            }
        } catch(Exception e){} return Command.SINGLE_SUCCESS;
    }
    
    private static int executeSetGenFull(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            String typeStr = StringArgumentType.getString(context, "type");
            Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
            if(arenaOpt.isPresent()) {
                Generator.Type type = Generator.Type.valueOf(typeStr.toUpperCase());
                String loc = LocationUtils.toString((ServerWorld)player.getEntityWorld(), player.getBlockPos(), player.getYaw(), player.getPitch());
                arenaOpt.get().addGenerator(new Generator(type, loc));
                BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
                context.getSource().sendMessage(Text.literal("Generator added.").formatted(Formatting.GREEN));
            }
        } catch(Exception e){} return Command.SINGLE_SUCCESS;
    }

    private static int executeTeamAddFull(CommandContext<ServerCommandSource> context) {
        String arenaName = StringArgumentType.getString(context, "arena");
        String teamName = StringArgumentType.getString(context, "teamName");
        String colorInput = StringArgumentType.getString(context, "color");
        Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
        if (arenaOpt.isPresent()) {
             Formatting color = Formatting.byName(colorInput.toUpperCase());
             if(color==null) color = Formatting.WHITE;
             arenaOpt.get().addTeam(new Team(teamName, color));
             BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
             context.getSource().sendMessage(Text.literal("Team added.").formatted(Formatting.GREEN));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeTeamRemoveFull(CommandContext<ServerCommandSource> context) {
         String arenaName = StringArgumentType.getString(context, "arena");
         String teamName = StringArgumentType.getString(context, "teamName");
         Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
         if (arenaOpt.isPresent()) {
             arenaOpt.get().getTeams().removeIf(t -> t.getName().equalsIgnoreCase(teamName));
             BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
             context.getSource().sendMessage(Text.literal("Team removed.").formatted(Formatting.GREEN));
         }
         return Command.SINGLE_SUCCESS;
    }

    private static int executeTeamSpawnFull(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            String teamName = StringArgumentType.getString(context, "teamName");
            Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
            if (arenaOpt.isPresent()) {
                Optional<Team> t = arenaOpt.get().getTeams().stream().filter(te -> te.getName().equalsIgnoreCase(teamName)).findFirst();
                if (t.isPresent()) {
                    String loc = LocationUtils.toString((ServerWorld)player.getEntityWorld(), player.getBlockPos());
                    t.get().setSpawnLocation(loc);
                    BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
                    context.getSource().sendMessage(Text.literal("Spawn set.").formatted(Formatting.GREEN));
                }
            }
        } catch(Exception e){} return Command.SINGLE_SUCCESS;
    }
    
    private static int executeTeamBedFull(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            String teamName = StringArgumentType.getString(context, "teamName");
            Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
            if (arenaOpt.isPresent()) {
                Optional<Team> t = arenaOpt.get().getTeams().stream().filter(te -> te.getName().equalsIgnoreCase(teamName)).findFirst();
                if (t.isPresent()) {
                    String loc = LocationUtils.toString((ServerWorld)player.getEntityWorld(), player.getBlockPos(), player.getYaw(), player.getPitch());
                    t.get().setBedLocation(loc);
                    BedWars.getInstance().getArenaManager().saveArena(arenaOpt.get());
                    context.getSource().sendMessage(Text.literal("Bed location set.").formatted(Formatting.GREEN));
                }
            }
        } catch(Exception e){} return Command.SINGLE_SUCCESS;
    }

    private static int executeJoinFull(CommandContext<ServerCommandSource> context) {
         try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String arenaName = StringArgumentType.getString(context, "arena");
            Optional<Arena> arenaOpt = BedWars.getInstance().getArenaManager().getArena(arenaName);
            if (arenaOpt.isPresent() && arenaOpt.get().addPlayer(player)) {
                context.getSource().sendMessage(Text.literal("Joined!").formatted(Formatting.GREEN));
            }
        } catch(Exception e){} return Command.SINGLE_SUCCESS;
    }
    
    private static int executeListFull(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.literal("--- Arenas ---").formatted(Formatting.YELLOW));
        for (Arena arena : BedWars.getInstance().getArenaManager().getArenas()) {
            context.getSource().sendMessage(Text.literal("- " + arena.getName()));
        }
        return Command.SINGLE_SUCCESS;
    }
}