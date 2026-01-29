package com.alexcreeper.bedwars1058.shop;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ShopContent {
    private final String id;
    private final int slot;
    private final ItemStack stack;
    private final Item currency;
    private final int cost;

    public ShopContent(String id, int slot, ItemStack stack, Item currency, int cost) {
        this.id = id;
        this.slot = slot;
        this.stack = stack;
        this.currency = currency;
        this.cost = cost;
    }

    public int getSlot() { return slot; }
    public ItemStack getStack() { return stack; }
    public Item getCurrency() { return currency; }
    public int getCost() { return cost; }
}