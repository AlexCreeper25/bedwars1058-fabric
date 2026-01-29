package com.alexcreeper.bedwars1058.api.configuration;

import com.alexcreeper.bedwars1058.BedWars;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {

    private final File file;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public ConfigManager(String fileName, Path dataDirectory) {
        this.file = new File(dataDirectory.toFile(), fileName);
        
        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (IOException e) {
                BedWars.LOGGER.error("Error creating configuration file: " + fileName, e);
            }
        }

        this.loader = YamlConfigurationLoader.builder().path(file.toPath()).build();
        reload();
    }

    public void reload() {
        try {
            this.root = loader.load();
        } catch (IOException e) {
            BedWars.LOGGER.error("Error loading configuration: " + file.getName(), e);
        }
    }

    public void save() {
        try {
            loader.save(root);
        } catch (IOException e) {
            BedWars.LOGGER.error("Error saving configuration: " + file.getName(), e);
        }
    }

    // --- Getters ---

    public String getString(String path) { 
        return root.node((Object[]) path.split("\\.")).getString(); 
    }
    
    public boolean getBoolean(String path) { 
        return root.node((Object[]) path.split("\\.")).getBoolean(); 
    }
    
    public int getInt(String path) { 
        return root.node((Object[]) path.split("\\.")).getInt(); 
    }

    // --- Setters ---

    public void set(String path, Object value) {
        try {
            // Split path by "." to handle "waiting.Loc" -> waiting node -> Loc node
            root.node((Object[]) path.split("\\.")).set(value);
            save(); // Auto-save for simplicity
        } catch (Exception e) {
            BedWars.LOGGER.error("Failed to set config value: " + path, e);
        }
    }
    
    public CommentedConfigurationNode getNode(String path) {
        return root.node((Object[]) path.split("\\."));
    }
}