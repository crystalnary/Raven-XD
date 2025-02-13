package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

public class Refill extends Module {
    private final SliderSetting speed;
    private long nextMoveTime = 0;
    private int noValidTickCounter = 0;

    public Refill() {
        super("Refill", category.player);
        DescriptionSetting description = new DescriptionSetting("Transfers soup/potion items to your hotbar. Keep inventory open until complete.");
        speed = new SliderSetting("Delay", 100, 50, 500, 10, "ms");
        this.registerSetting(description, speed);
    }

    @Override
    public void onEnable() {
        nextMoveTime = System.currentTimeMillis();
        noValidTickCounter = 0;

        if (countEmptyHotbarSlotsPlayer() == 0) {
            this.disable();
            return;
        }

        if (!hasValidHealingItemsInPlayer()) {
            this.disable();
            return;
        }

        if (!(mc.currentScreen instanceof GuiInventory))
            toggleInventory(true);
    }

    @Override
    public void onDisable() {
        nextMoveTime = 0;

        if (mc.currentScreen instanceof GuiInventory)
            mc.thePlayer.closeScreen();
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null) {
            return;
        }

        if (!(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        Container container = mc.thePlayer.openContainer;
        if (container == null) {
            return;
        }

        if (countEmptyHotbarSlots(container) == 0) {
            this.disable();
            return;
        }

        if (System.currentTimeMillis() < nextMoveTime) {
            return;
        }

        int sourceSlot = findValidItemInMainInventory(container);
        if (sourceSlot == -1) {
            noValidTickCounter++;
            int NO_VALID_THRESHOLD = 5;
            if (noValidTickCounter >= NO_VALID_THRESHOLD) {
                this.disable();
            }
            return;
        } else {
            noValidTickCounter = 0;
        }

        mc.playerController.windowClick(container.windowId, sourceSlot, 0, 1, mc.thePlayer);
        nextMoveTime = System.currentTimeMillis() + (long) speed.getInput();
    }

    private int countEmptyHotbarSlots(Container container) {
        int count = 0;
        for (int i = 36; i <= 44; i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && !slot.getHasStack()) {
                count++;
            }
        }
        return count;
    }

    private int countEmptyHotbarSlotsPlayer() {
        int count = 0;
        InventoryPlayer inv = mc.thePlayer.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null) {
                count++;
            }
        }
        return count;
    }

    private int findValidItemInMainInventory(Container container) {
        for (int i = 9; i <= 35; i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (isSoup(stack) || isHealingPotion(stack)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean hasValidHealingItemsInPlayer() {
        InventoryPlayer inv = mc.thePlayer.inventory;
        for (int i = 9; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && (isSoup(stack) || isHealingPotion(stack))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSoup(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        String name = stack.getItem().getUnlocalizedName().toLowerCase();
        return name.contains("soup") || name.contains("stew");
    }

    private boolean isHealingPotion(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemPotion)) {
            return false;
        }
        ItemPotion potion = (ItemPotion) stack.getItem();
        if (!potion.isSplash(stack.getItemDamage())) {
            return false;
        }
        if (potion.getEffects(stack) != null) {
            for (PotionEffect effect : potion.getEffects(stack)) {
                if (effect.getPotionID() == Potion.heal.id) {
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleInventory(boolean open) {
        if (open && (mc.currentScreen instanceof GuiInventory)) return;
        if (!open && !(mc.currentScreen instanceof GuiInventory)) return;

        int inventoryKeyCode = mc.gameSettings.keyBindInventory.getKeyCode();
        KeyBinding.setKeyBindState(inventoryKeyCode, true);
        KeyBinding.onTick(inventoryKeyCode);
        KeyBinding.setKeyBindState(inventoryKeyCode, false);
    }
}