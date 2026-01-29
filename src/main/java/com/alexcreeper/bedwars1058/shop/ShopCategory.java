package com.alexcreeper.bedwars1058.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ShopCategory {
    private final String id;
    private final int slot;
    private final ItemStack icon;
    private final Text name;
    private final List<ShopContent> content = new ArrayList<>();

    public ShopCategory(String id, int slot, ItemStack icon, Text name) {
        this.id = id;
        this.slot = slot;
        this.icon = icon;
        this.name = name;
    }

    public void addContent(ShopContent item) {
        this.content.add(item);
    }

    public String getId() { return id; }
    public int getSlot() { return slot; }
    public ItemStack getIcon() { return icon; }
    public Text getName() { return name; }
    public List<ShopContent> getContent() { return content; }
}