package com.alexcreeper.bedwars1058.api.configuration;

import com.alexcreeper.bedwars1058.BedWars;
import net.minecraft.text.Text;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Language {

    private static final Map<String, String> messages = new HashMap<>();
    private static File file;

    public static void init() {
        file = new File(BedWars.getInstance().getDataDirectory().toFile(), "messages.yml");
        if (!file.exists()) saveDefault();
        load();
    }

    private static void load() {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
        try {
            CommentedConfigurationNode root = loader.load();
            loadNodes(root, "");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadNodes(CommentedConfigurationNode node, String path) {
        if (node.isMap()) {
            for (Map.Entry<Object, CommentedConfigurationNode> entry : node.childrenMap().entrySet()) {
                String newPath = path.isEmpty() ? entry.getKey().toString() : path + "." + entry.getKey().toString();
                loadNodes(entry.getValue(), newPath);
            }
        } else {
            messages.put(path, node.getString(""));
        }
    }

    public static Text getMsg(String path) {
        String msg = messages.getOrDefault(path, path);
        return Text.literal(msg.replace("&", "\u00a7"));
    }

    private static void saveDefault() {
        try {
            file.createNewFile();
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
            CommentedConfigurationNode root = loader.createNode();

            root.node("prefix").set("&7[&cBedWars&7] ");
            root.node("ingame", "game-start").set("&a&lTHE GAME HAS STARTED!");
            root.node("ingame", "game-start-countdown").set("&eThe game starts in {0} seconds!");
            root.node("ingame", "not-enough-players").set("&cNot enough players to start.");
            root.node("ingame", "shop", "purchased").set("&aPurchased {0}!");
            root.node("ingame", "shop", "not-enough-money").set("&cYou don't have enough {0}!");
            root.node("ingame", "messages", "bed-destroyed").set("&c&lBED DESTRUCTION > &7Team {0}'s bed was destroyed by {1}!");
            root.node("ingame", "messages", "player-die").set("&c{0} died.");
            root.node("ingame", "messages", "respawning").set("&eRespawning in {0} seconds...");
            root.node("ingame", "messages", "respawned").set("&aYou have respawned!");
            root.node("ingame", "messages", "eliminated").set("&c&lELIMINATED > &7{0} was eliminated!");
            root.node("ingame", "messages", "win").set("&a&lTEAM {0} WINS!");

            loader.save(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}