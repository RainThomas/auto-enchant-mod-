package com.example.autoenchant;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
public class AutoEnchantMod implements ClientModInitializer {
    private static final int ENCHANT_BUTTON_INDEX = 0;
    private static final int MIN_LEVEL_EXCLUSIVE = 50;
    private static final int MIN_LAPIS_TO_START = 28;
    private static final int COOLDOWN_TICKS = 2;
    private int cooldown = 0;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (cooldown > 0) { cooldown--; return; }
            Screen screen = client.currentScreen;
            if (!(screen instanceof HandledScreen<?>)) return;
            ScreenHandler handler = client.player.currentScreenHandler;
            if (handler instanceof EnchantmentScreenHandler) runEnchantTick(client, (EnchantmentScreenHandler) handler);
            else if (handler instanceof GrindstoneScreenHandler) runGrindTick(client, (GrindstoneScreenHandler) handler);
        });
    }
    private void runEnchantTick(MinecraftClient client, EnchantmentScreenHandler handler) {
        if (client.player.experienceLevel <= MIN_LEVEL_EXCLUSIVE) return;
        if (countItemInMain(handler, Items.LAPIS_LAZULI) < MIN_LAPIS_TO_START) return;
        if (!handler.getSlot(0).hasStack()) {
            int from = findFirstEnchantableInMain(handler);
            if (from != -1) { moveViaPickup(handler, from, 0); cooldown = COOLDOWN_TICKS; }
            return;
        }
        if (!handler.getSlot(1).hasStack()) {
            int lap = findFirstInMain(handler, Items.LAPIS_LAZULI);
            if (lap != -1) { moveViaPickup(handler, lap, 1); cooldown = COOLDOWN_TICKS; }
            return;
        }
        client.interactionManager.clickButton(handler.syncId, ENCHANT_BUTTON_INDEX);
        cooldown = 4;
        if (handler.getSlot(0).hasStack()) {
            int target = findFirstEmptyMainSlot(handler);
            if (target != -1) moveViaPickup(handler, 0, target);
        }
    }
    private void runGrindTick(MinecraftClient client, GrindstoneScreenHandler handler) {
        if (!handler.getSlot(0).hasStack()) {
            int from = findFirstEnchantedInMain(handler);
            if (from != -1) { moveViaPickup(handler, from, 0); cooldown = COOLDOWN_TICKS; }
            return;
        }
        if (handler.getSlot(2).hasStack()) {
            int target = findFirstEmptyMainSlot(handler);
            if (target != -1) { moveViaPickup(handler, 2, target); cooldown = COOLDOWN_TICKS; }
            if (handler.getSlot(0).hasStack()) moveToFirstEmptyMain(handler, 0);
            if (handler.getSlot(1).hasStack()) moveToFirstEmptyMain(handler, 1);
            return;
        }
    }
    private void click(ScreenHandler handler, int slot, int button, SlotActionType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.interactionManager.clickSlot(handler.syncId, slot, button, type, client.player);
    }
    private void moveViaPickup(ScreenHandler handler, int from, int to) { click(handler, from, 0, SlotActionType.PICKUP); click(handler, to, 0, SlotActionType.PICKUP); }
    private void moveToFirstEmptyMain(ScreenHandler handler, int from) { int t = findFirstEmptyMainSlot(handler); if (t != -1) moveViaPickup(handler, from, t); }
    private int countItemInMain(ScreenHandler handler, Item item) {
        int c = 0; for (int i=0;i<handler.slots.size();i++){ Slot s = handler.getSlot(i);
        if (isMainInventorySlot(s) && s.hasStack() && s.getStack().isOf(item)) c += s.getStack().getCount(); } return c; }
    private int findFirstInMain(ScreenHandler handler, Item item) {
        for (int i=0;i<handler.slots.size();i++){ Slot s=handler.getSlot(i);
        if (isMainInventorySlot(s) && s.hasStack() && s.getStack().isOf(item)) return i; } return -1; }
    private int findFirstEnchantableInMain(ScreenHandler handler) {
        for (int i=0;i<handler.slots.size();i++){ Slot s=handler.getSlot(i);
        if (!isMainInventorySlot(s) || !s.hasStack()) continue; ItemStack st=s.getStack();
        if (st.isEnchantable() && !st.hasEnchantments() && !st.isOf(Items.ENCHANTED_BOOK)) return i; } return -1; }
    private int findFirstEnchantedInMain(ScreenHandler handler) {
        for (int i=0;i<handler.slots.size();i++){ Slot s=handler.getSlot(i);
        if (!isMainInventorySlot(s) || !s.hasStack()) continue; if (s.getStack().hasEnchantments()) return i; } return -1; }
    private boolean isMainInventorySlot(Slot s){ return s.inventory instanceof PlayerInventory && s.getIndex()>=9 && s.getIndex()<36; }
    private int findFirstEmptyMainSlot(ScreenHandler handler){ for (int i=0;i<handler.slots.size();i++){ Slot s=handler.getSlot(i);
        if (isMainInventorySlot(s) && !s.hasStack()) return i; } return -1; }
}
