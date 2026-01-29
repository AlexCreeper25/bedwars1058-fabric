package com.alexcreeper.bedwars1058.arena;

import com.alexcreeper.bedwars1058.api.configuration.ConfigManager;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String name;
    
    private String shopFileName;
    private String upgradeFileName;
    
    private List<String> scoreboardWaiting = new ArrayList<>();
    private List<String> scoreboardStarting = new ArrayList<>();
    private List<String> scoreboardPlaying = new ArrayList<>();

    public Group(String name, ConfigManager config) {
        this.name = name;
        loadSettings(config);
    }
    
    private void loadSettings(ConfigManager config) {
        this.shopFileName = config.getString("shop-settings.file");
        if (shopFileName == null) shopFileName = "shop.yml";
        
        this.upgradeFileName = config.getString("team-upgrades-settings.file");
        if (upgradeFileName == null) upgradeFileName = "upgrades.yml";
        
        scoreboardWaiting = getList(config.getNode("scoreboard.waiting"));
        scoreboardStarting = getList(config.getNode("scoreboard.starting"));
        scoreboardPlaying = getList(config.getNode("scoreboard.playing"));
    }
    
    private List<String> getList(CommentedConfigurationNode node) {
        List<String> list = new ArrayList<>();
        if (node.virtual()) return list;
        try {
            List<? extends String> raw = node.getList(String.class);
            if (raw != null) list.addAll(raw);
        } catch (Exception e) {}
        return list;
    }

    public String getName() { return name; }
    public String getShopFileName() { return shopFileName; }
    public List<String> getScoreboardWaiting() { return scoreboardWaiting; }
    public List<String> getScoreboardStarting() { return scoreboardStarting; }
    public List<String> getScoreboardPlaying() { return scoreboardPlaying; }
}