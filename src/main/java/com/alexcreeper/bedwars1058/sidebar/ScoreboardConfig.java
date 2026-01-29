package com.alexcreeper.bedwars1058.sidebar;

import com.alexcreeper.bedwars1058.BedWars;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardConfig {

    private static File file;
    
    // Cache the lines
    public static List<String> waitingLines = new ArrayList<>();
    public static List<String> startingLines = new ArrayList<>();
    public static List<String> playingLines = new ArrayList<>();
    public static String title = "BED WARS";

    public static void init() {
        file = new File(BedWars.getInstance().getDataDirectory().toFile(), "scoreboard.yml");
        if (!file.exists()) {
            saveDefault();
        }
        load();
    }

    private static void load() {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
        try {
            CommentedConfigurationNode root = loader.load();
            
            // Standard BedWars1058 keys
            title = root.node("scoreboard", "title").getString("BED WARS");
            
            waitingLines = getList(root.node("scoreboard-waiting"));
            startingLines = getList(root.node("scoreboard-starting"));
            playingLines = getList(root.node("scoreboard-playing"));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static List<String> getList(CommentedConfigurationNode node) {
        List<String> list = new ArrayList<>();
        if (node.virtual()) return list;
        try {
            // Configurate handles lists differently depending on version, generic retrieval:
            List<? extends String> raw = node.getList(String.class);
            if (raw != null) list.addAll(raw);
        } catch (Exception e) {}
        return list;
    }

    private static void saveDefault() {
        try {
            file.createNewFile();
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
            CommentedConfigurationNode root = loader.createNode();

            root.node("scoreboard", "title").set("&e&lBED WARS");
            
            // Waiting
            List<String> waiting = new ArrayList<>();
            waiting.add("&7{date}");
            waiting.add("");
            waiting.add("Map: &a{map}");
            waiting.add("Players: &a{on}/{max}");
            waiting.add("");
            waiting.add("&eWaiting...");
            waiting.add("");
            waiting.add("&ewww.example.com");
            root.node("scoreboard-waiting").setList(String.class, waiting);
            
            // Starting
            List<String> starting = new ArrayList<>();
            starting.add("&7{date}");
            starting.add("");
            starting.add("Map: &a{map}");
            starting.add("Players: &a{on}/{max}");
            starting.add("");
            starting.add("&eStarting in &c{time}s");
            starting.add("");
            starting.add("&ewww.example.com");
            root.node("scoreboard-starting").setList(String.class, starting);

            // Playing
            List<String> playing = new ArrayList<>();
            playing.add("&7{date}");
            playing.add("");
            playing.add("&fNext Event:");
            playing.add("&a{next_event} &e{next_event_time}");
            playing.add("");
            playing.add("{team_R} &fRed: {team_status_Red}");
            playing.add("{team_B} &fBlue: {team_status_Blue}");
            playing.add("{team_G} &fGreen: {team_status_Green}");
            playing.add("{team_Y} &fYellow: {team_status_Yellow}");
            playing.add("");
            playing.add("&ewww.example.com");
            root.node("scoreboard-playing").setList(String.class, playing);

            loader.save(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}