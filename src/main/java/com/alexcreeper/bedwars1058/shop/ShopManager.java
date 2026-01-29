package com.alexcreeper.bedwars1058.shop;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private static final Map<String, List<ShopCategory>> shops = new HashMap<>();

    public static void init() {
        File defaultShop = new File(BedWars.getInstance().getDataDirectory().toFile(), "shop.yml");
        if (!defaultShop.exists()) saveDefault();
    }

    public static List<ShopCategory> getShopForGroup(String fileName) {
        if (!shops.containsKey(fileName)) loadShop(fileName);
        return shops.getOrDefault(fileName, new ArrayList<>());
    }

    private static void loadShop(String fileName) {
        File file = new File(BedWars.getInstance().getDataDirectory().toFile(), fileName);
        if (!file.exists()) return;

        List<ShopCategory> categories = new ArrayList<>();
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
        try {
            CommentedConfigurationNode root = loader.load();
            for (Map.Entry<Object, CommentedConfigurationNode> entry : root.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                if (key.endsWith("-category")) categories.add(loadCategory(key, entry.getValue()));
            }
            shops.put(fileName, categories);
            BedWars.LOGGER.info("Loaded shop: " + fileName + " (" + categories.size() + " categories)");
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private static ShopCategory loadCategory(String id, CommentedConfigurationNode node) {
        int catSlot = node.node("category-slot").getInt();
        String iconMat = node.node("category-item", "material").getString("stone");
        String displayName = node.node("category-item", "name").getString(id);
        
        Item iconItem = Registries.ITEM.get(Identifier.of("minecraft", iconMat.toLowerCase()));
        if (iconItem == null) iconItem = Items.STONE;
        
        ItemStack iconStack = new ItemStack(iconItem);
        iconStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName.replace("&", "\u00a7")));

        ShopCategory category = new ShopCategory(id, catSlot, iconStack, Text.literal(displayName.replace("&", "\u00a7")));
        CommentedConfigurationNode contentNode = node.node("category-content");
        for (Map.Entry<Object, CommentedConfigurationNode> itemEntry : contentNode.childrenMap().entrySet()) {
            loadContent(category, itemEntry.getKey().toString(), itemEntry.getValue());
        }
        return category;
    }
    
    private static void loadContent(ShopCategory category, String id, CommentedConfigurationNode node) {
        int slot = node.node("content-settings", "content-slot").getInt();
        CommentedConfigurationNode tier1 = node.node("content-tiers", "tier1");
        if (!tier1.virtual()) {
            String matStr = tier1.node("tier-item", "material").getString("stone");
            int amount = tier1.node("tier-item", "amount").getInt(1);
            int cost = tier1.node("tier-settings", "cost").getInt();
            String currencyStr = tier1.node("tier-settings", "currency").getString("iron");
            
            Item mat = Registries.ITEM.get(Identifier.of("minecraft", matStr.toLowerCase()));
            Item curr = parseCurrency(currencyStr);
            if (mat != null) category.addContent(new ShopContent(id, slot, new ItemStack(mat, amount), curr, cost));
        }
    }
    
    private static Item parseCurrency(String name) {
        if (name.equalsIgnoreCase("iron")) return Items.IRON_INGOT;
        if (name.equalsIgnoreCase("gold")) return Items.GOLD_INGOT;
        if (name.equalsIgnoreCase("diamond")) return Items.DIAMOND;
        if (name.equalsIgnoreCase("emerald")) return Items.EMERALD;
        return Items.IRON_INGOT;
    }

    private static void saveDefault() {
        try {
            File file = new File(BedWars.getInstance().getDataDirectory().toFile(), "shop.yml");
            file.createNewFile();
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
            CommentedConfigurationNode root = loader.createNode();

            CommentedConfigurationNode blocks = root.node("blocks-category");
            blocks.node("category-slot").set(0);
            blocks.node("category-item", "material").set("white_wool");
            blocks.node("category-item", "name").set("&aBlocks");
            CommentedConfigurationNode wool = blocks.node("category-content", "wool");
            wool.node("content-settings", "content-slot").set(19);
            wool.node("content-tiers", "tier1", "tier-item", "material").set("white_wool");
            wool.node("content-tiers", "tier1", "tier-item", "amount").set(16);
            wool.node("content-tiers", "tier1", "tier-settings", "cost").set(4);
            wool.node("content-tiers", "tier1", "tier-settings", "currency").set("iron");
            
            CommentedConfigurationNode melee = root.node("melee-category");
            melee.node("category-slot").set(1);
            melee.node("category-item", "material").set("golden_sword");
            melee.node("category-item", "name").set("&cWeapons");
            CommentedConfigurationNode sword = melee.node("category-content", "stone_sword");
            sword.node("content-settings", "content-slot").set(19);
            sword.node("content-tiers", "tier1", "tier-item", "material").set("stone_sword");
            sword.node("content-tiers", "tier1", "tier-item", "amount").set(1);
            sword.node("content-tiers", "tier1", "tier-settings", "cost").set(10);
            sword.node("content-tiers", "tier1", "tier-settings", "currency").set("iron");

            loader.save(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}