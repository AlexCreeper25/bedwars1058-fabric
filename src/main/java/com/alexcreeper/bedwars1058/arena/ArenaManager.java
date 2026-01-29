package com.alexcreeper.bedwars1058.arena;

import com.alexcreeper.bedwars1058.BedWars;
import com.alexcreeper.bedwars1058.api.configuration.ConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArenaManager {

    private final List<Arena> arenas = new ArrayList<>();
    private final File arenasFolder;

    public ArenaManager() {
        this.arenasFolder = new File(BedWars.getInstance().getDataDirectory().toFile(), "Arenas");
        if (!arenasFolder.exists()) arenasFolder.mkdirs();
    }

    public void loadArenas() {
        arenas.clear();
        File[] files = arenasFolder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.getName().endsWith(".yml")) loadArenaFromFile(file);
        }
    }

    private void loadArenaFromFile(File file) {
        String arenaName = file.getName().replace(".yml", "");
        ConfigManager cfg = new ConfigManager(file.getName(), arenasFolder.toPath());
        
        String worldName = cfg.getString("general.world");
        if (worldName == null) worldName = arenaName;
        
        ServerWorld loadedWorld = BedWars.getInstance().getWorldAdapter().loadDynamicWorld(worldName);
        if (loadedWorld == null) return;
        
        Arena arena = new Arena(arenaName, worldName);
        arena.setWorld(loadedWorld);
        
        // Group Logic
        String groupName = cfg.getString("general.group");
        Group group = (groupName != null) ? BedWars.getInstance().getGroupManager().getGroup(groupName) : null;
        if (group == null) group = BedWars.getInstance().getGroupManager().getDefaultGroup();
        arena.setGroup(group);

        arena.setLobbySpawnLocation(cfg.getString("waiting.Loc"));
        arena.setMaxPlayers(cfg.getInt("general.max-players"));
        arena.setMinPlayers(cfg.getInt("general.min-players"));
        
        if (arena.getMaxPlayers() == 0) arena.setMaxPlayers(16);
        if (arena.getMinPlayers() == 0) arena.setMinPlayers(2);
        
        CommentedConfigurationNode teamsNode = cfg.getNode("Team");
        if (!teamsNode.virtual()) { 
            for (Object key : teamsNode.childrenMap().keySet()) {
                String teamName = key.toString();
                String path = "Team." + teamName;
                Formatting color = Formatting.byName(cfg.getString(path + ".Color"));
                if (color == null) color = Formatting.WHITE;
                
                Team team = new Team(teamName, color);
                team.setSpawnLocation(cfg.getString(path + ".Spawn"));
                team.setBedLocation(cfg.getString(path + ".Bed"));
                team.setShopLocation(cfg.getString(path + ".Shop"));
                arena.addTeam(team);
            }
        }
        
        CommentedConfigurationNode gensNode = cfg.getNode("Generator");
        if (!gensNode.virtual()) {
            for (Object key : gensNode.childrenMap().keySet()) {
                String index = key.toString();
                try {
                    Generator.Type type = Generator.Type.valueOf(cfg.getString("Generator." + index + ".Type"));
                    Generator gen = new Generator(type, cfg.getString("Generator." + index + ".Loc"));
                    arena.addGenerator(gen);
                } catch (Exception e) {}
            }
        }
        this.arenas.add(arena);
    }

    public boolean createArena(String arenaName, String mapName, ServerPlayerEntity creator) {
        if (getArena(arenaName).isPresent()) return false;
        ServerWorld world = BedWars.getInstance().getWorldAdapter().loadDynamicWorld(mapName);
        if (world == null) return false; 
        Arena arena = new Arena(arenaName, mapName);
        arena.setWorld(world);
        arena.setGroup(BedWars.getInstance().getGroupManager().getDefaultGroup());
        arenas.add(arena);
        saveArena(arena);
        creator.teleport(world, 0.5, 100, 0.5, java.util.Collections.emptySet(), 0f, 0f, false);
        creator.changeGameMode(net.minecraft.world.GameMode.CREATIVE);
        return true;
    }

    public void deleteArena(String name) {
        Optional<Arena> arenaOpt = getArena(name);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            BedWars.getInstance().getWorldAdapter().unloadWorld(arena.getWorldName());
            new File(arenasFolder, name + ".yml").delete();
            arenas.remove(arena);
        }
    }

    public void saveArena(Arena arena) {
        ConfigManager cfg = new ConfigManager(arena.getName() + ".yml", arenasFolder.toPath());
        cfg.set("general.world", arena.getWorldName());
        if (arena.getGroup() != null) cfg.set("general.group", arena.getGroup().getName());
        cfg.set("general.max-players", arena.getMaxPlayers());
        cfg.set("general.min-players", arena.getMinPlayers());
        cfg.set("waiting.Loc", arena.getLobbySpawnLocation());
        for (Team team : arena.getTeams()) {
            String path = "Team." + team.getName();
            cfg.set(path + ".Color", team.getColor().name());
            cfg.set(path + ".Spawn", team.getSpawnLocation());
            cfg.set(path + ".Bed", team.getBedLocation());
            cfg.set(path + ".Shop", team.getShopLocation());
        }
        cfg.set("Generator", null); 
        int genIndex = 0;
        for (Generator gen : arena.getGenerators()) {
            String path = "Generator." + genIndex;
            cfg.set(path + ".Type", gen.getType().name());
            cfg.set(path + ".Loc", gen.getLocationString());
            genIndex++;
        }
        cfg.save();
    }

    public Optional<Arena> getArena(String name) { return arenas.stream().filter(a -> a.getName().equals(name)).findFirst(); }
    public List<Arena> getArenas() { return new ArrayList<>(arenas); }
}