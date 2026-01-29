package com.alexcreeper.bedwars1058.upgrades;

import com.alexcreeper.bedwars1058.BedWars;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpgradeManager {
    
    // We store config nodes or simple objects. For simplicity, we store the full config structure in objects.
    public static class TierUpgrade {
        String key;
        int slot;
        ItemStack icon;
        List<Tier> tiers = new ArrayList<>();
        
        public static class Tier {
            int cost;
            Item currency;
            String name;
        }
    }

    private static final List<TierUpgrade> upgrades = new ArrayList<>();
    private static File file;

    public static void init() {
        file = new File(BedWars.getInstance().getDataDirectory().toFile(), "upgrades.yml");
        if (!file.exists()) saveDefault();
        load();
    }

    private static void load() {
        upgrades.clear();
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
        try {
            CommentedConfigurationNode root = loader.load();
            for (Map.Entry<Object, CommentedConfigurationNode> entry : root.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                // Skip non-upgrade keys if any
                if (key.equals("menu-size") || key.equals("menu-title")) continue;
                
                loadUpgrade(key, entry.getValue());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private static void loadUpgrade(String key, CommentedConfigurationNode node) {
        TierUpgrade up = new TierUpgrade();
        up.key = key;
        up.slot = node.node("slot").getInt(-1);
        if (up.slot == -1) return;
        
        // Load Tiers (tier-1, tier-2, etc.)
        int i = 1;
        while (true) {
            CommentedConfigurationNode tierNode = node.node("tier-" + i);
            if (tierNode.virtual()) break;
            
            TierUpgrade.Tier t = new TierUpgrade.Tier();
            t.cost = tierNode.node("cost").getInt();
            String curr = tierNode.node("currency").getString("diamond");
            t.currency = parseCurrency(curr);
            t.name = tierNode.node("name").getString(key + " " + i);
            
            up.tiers.add(t);
            i++;
        }
        
        // Icon (Usually based on Tier 1 info, simplified here)
        up.icon = new ItemStack(Items.ENCHANTED_BOOK); // Default
        // In full impl, read 'display-item' from tier-1
        
        upgrades.add(up);
    }
    
    private static Item parseCurrency(String name) {
        if (name.equalsIgnoreCase("iron")) return Items.IRON_INGOT;
        if (name.equalsIgnoreCase("gold")) return Items.GOLD_INGOT;
        if (name.equalsIgnoreCase("emerald")) return Items.EMERALD;
        return Items.DIAMOND;
    }
    
    public static List<TierUpgrade> getUpgrades() { return upgrades; }

    private static void saveDefault() {
        try {
            file.createNewFile();
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
            CommentedConfigurationNode root = loader.createNode();
            
            // Sharpness
            CommentedConfigurationNode sharp = root.node("sharpness");
            sharp.node("slot").set(10);
            sharp.node("tier-1", "cost").set(4);
            sharp.node("tier-1", "currency").set("diamond");
            sharp.node("tier-1", "name").set("&cSharpness");
            
            // Protection
            CommentedConfigurationNode prot = root.node("protection");
            prot.node("slot").set(11);
            prot.node("tier-1", "cost").set(2);
            prot.node("tier-1", "currency").set("diamond");
            prot.node("tier-1", "name").set("&cProtection I");
            prot.node("tier-2", "cost").set(4);
            prot.node("tier-2", "currency").set("diamond");
            prot.node("tier-2", "name").set("&cProtection II");
            
            // Haste
            CommentedConfigurationNode haste = root.node("haste");
            haste.node("slot").set(12);
            haste.node("tier-1", "cost").set(2);
            haste.node("tier-1", "currency").set("diamond");
            haste.node("tier-1", "name").set("&eHaste I");
            
            // Forge
            CommentedConfigurationNode forge = root.node("forge");
            forge.node("slot").set(13);
            forge.node("tier-1", "cost").set(2);
            forge.node("tier-1", "currency").set("diamond");
            forge.node("tier-1", "name").set("&cIron Forge");

            // Heal Pool
            CommentedConfigurationNode heal = root.node("heal-pool");
            heal.node("slot").set(14);
            heal.node("tier-1", "cost").set(1);
            heal.node("tier-1", "currency").set("diamond");
            heal.node("tier-1", "name").set("&aHeal Pool");

            loader.save(root);
        } catch (Exception e) {}
    }
}