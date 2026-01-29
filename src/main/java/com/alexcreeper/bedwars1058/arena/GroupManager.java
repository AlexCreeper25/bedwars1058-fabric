package com.alexcreeper.bedwars1058.arena;

import com.alexcreeper.bedwars1058.BedWars;
import com.alexcreeper.bedwars1058.api.configuration.ConfigManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupManager {

    private final Map<String, Group> groups = new HashMap<>();
    private final File groupsFolder;
    private Group defaultGroup;

    public GroupManager() {
        this.groupsFolder = new File(BedWars.getInstance().getDataDirectory().toFile(), "Groups");
        if (!groupsFolder.exists()) {
            groupsFolder.mkdirs();
            createDefaultGroup();
        }
    }
    
    public void loadGroups() {
        groups.clear();
        File[] files = groupsFolder.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.getName().endsWith(".yml")) {
                String name = file.getName().replace(".yml", "");
                ConfigManager cfg = new ConfigManager(file.getName(), groupsFolder.toPath());
                groups.put(name, new Group(name, cfg));
                BedWars.LOGGER.info("Loaded group: " + name);
            }
        }
        
        if (groups.containsKey("Solo")) defaultGroup = groups.get("Solo");
        else if (!groups.isEmpty()) defaultGroup = groups.values().iterator().next();
    }
    
    private void createDefaultGroup() {
        ConfigManager cfg = new ConfigManager("Solo.yml", groupsFolder.toPath());
        cfg.set("shop-settings.file", "shop.yml");
        cfg.set("team-upgrades-settings.file", "upgrades.yml");
        
        List<String> waiting = new ArrayList<>();
        waiting.add("&7{date}");
        waiting.add("&eWaiting...");
        waiting.add("Map: &a{map}");
        waiting.add("Group: &7Solo");
        cfg.set("scoreboard.waiting", waiting);
        
        List<String> starting = new ArrayList<>();
        starting.add("&7{date}");
        starting.add("&eStarting in {time}...");
        starting.add("Map: &a{map}");
        starting.add("Group: &7Solo");
        cfg.set("scoreboard.starting", starting);

        List<String> playing = new ArrayList<>();
        playing.add("&7{date}");
        playing.add("&fNext Event: &a{next_event}");
        playing.add("{team_R} &fRed: {team_status_Red}");
        playing.add("{team_B} &fBlue: {team_status_Blue}");
        cfg.set("scoreboard.playing", playing);
        
        cfg.save();
    }

    public Group getGroup(String name) { return groups.get(name); }
    public Group getDefaultGroup() { return defaultGroup; }
}