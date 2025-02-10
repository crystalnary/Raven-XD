package keystrokesmod.module.impl.player;

import keystrokesmod.eventbus.annotations.EventListener;
import keystrokesmod.event.render.Render2DEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ContainerUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public class InstaHeal extends Module {
    private final SliderSetting mode;
    private final SliderSetting wastePercentage;

    private int itemsToUse;
    private int originalSlot = -1;
    private int targetSlot = -1;
    private long lastActionTime;
    private float healPerItem;
    private State currentState = State.NONE;

    private enum State {
        NONE,
        SWITCHING,
        USING,
        SWITCHING_BACK
    }

    public InstaHeal() {
        super("InstaHeal", Module.category.player);
        this.registerSetting(new DescriptionSetting("Optimizes healing items usage."));
        this.registerSetting(mode = new SliderSetting("Mode", 0, 0, 1, 1));
        this.registerSetting(wastePercentage = new SliderSetting("Waste %", 60, 0, 100, 1));
    }

    @Override
    public void onEnable() {
        if (!Utils.nullCheck() || mc.thePlayer.isDead) {
            this.disable();
            return;
        }

        if (mode.getInput() == 0) {
            targetSlot = ContainerUtils.getSlot(ItemSoup.class);
            if (targetSlot == -1) {
                this.disable();
                return;
            }
            healPerItem = 7;
        } else {
            int maxHeal = 0;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
                if (stack != null && isHealingPotion(stack)) {
                    int heal = getPotionHealAmount(stack);
                    if (heal > maxHeal) {
                        maxHeal = heal;
                        targetSlot = slot;
                    }
                }
            }
            if (targetSlot == -1) {
                this.disable();
                return;
            }
            healPerItem = maxHeal;
        }

        itemsToUse = calculateItemsToUse();
        if (itemsToUse <= 0) {
            this.disable();
            return;
        }

        originalSlot = mc.thePlayer.inventory.currentItem;
        currentState = State.SWITCHING;
        lastActionTime = System.currentTimeMillis();
    }

    private int calculateItemsToUse() {
        if (targetSlot == -1) return 0;
        float maxHealth = mc.thePlayer.getMaxHealth();
        float currentHealth = mc.thePlayer.getHealth();
        float needed = maxHealth - currentHealth;
        if (needed <= 0) return 0;

        int maxItemsAvailable = mc.thePlayer.inventory.getStackInSlot(targetSlot).stackSize;
        int bestN = 0;
        float allowedWaste = (float) wastePercentage.getInput();

        for (int n = 1; n <= maxItemsAvailable; n++) {
            float totalHeal = n * healPerItem;
            if (totalHeal >= needed && (totalHeal - needed) / totalHeal * 100 <= allowedWaste) {
                bestN = n;
                break;
            }
        }

        if (bestN == 0) {
            float bestEffective = 0;
            for (int n = 1; n <= maxItemsAvailable; n++) {
                float totalHeal = n * healPerItem;
                float effective = Math.min(totalHeal, needed);
                float waste = (totalHeal - effective) / totalHeal * 100;
                if (waste <= allowedWaste && effective > bestEffective) {
                    bestEffective = effective;
                    bestN = n;
                }
            }
        }

        return bestN;
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || currentState == State.NONE) return;
        if (mc.currentScreen instanceof GuiInventory) return;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastActionTime;

        switch (currentState) {
            case SWITCHING:
                if (elapsed >= 100) {
                    SlotHandler.setCurrentSlot(targetSlot);
                    currentState = State.USING;
                    lastActionTime = currentTime;
                }
                break;

            case USING:
                if (elapsed >= 50) {
                    ItemStack stack = SlotHandler.getHeldItem();
                    // If the stack is null or invalid, decrement the count to keep using the next items
                    if (stack == null || !isValidItem(stack)) {
                        itemsToUse--;
                    } else {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, stack);
                        itemsToUse--;
                    }
                    lastActionTime = currentTime;
                }
                if (itemsToUse <= 0) {
                    currentState = State.SWITCHING_BACK;
                }
                break;

            case SWITCHING_BACK:
                if (elapsed >= 100) {
                    SlotHandler.setCurrentSlot(originalSlot);
                    this.disable();
                }
                break;
        }
    }

    private boolean isValidItem(ItemStack stack) {
        if (mode.getInput() == 0) {
            return stack.getItem() instanceof ItemSoup;
        }
        return isHealingPotion(stack);
    }

    private boolean isHealingPotion(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemPotion)) return false;
        ItemPotion potion = (ItemPotion) stack.getItem();
        if (!potion.isSplash(stack.getItemDamage())) return false;

        List<PotionEffect> effects = potion.getEffects(stack);
        for (PotionEffect effect : effects) {
            if (effect.getPotionID() == Potion.heal.id) {
                return true;
            }
        }
        return false;
    }

    private int getPotionHealAmount(ItemStack stack) {
        List<PotionEffect> effects = ((ItemPotion) stack.getItem()).getEffects(stack);
        for (PotionEffect effect : effects) {
            if (effect.getPotionID() == Potion.heal.id) {
                return 4 * (effect.getAmplifier() + 1);
            }
        }
        return 0;
    }

    @Override
    public void onDisable() {
        currentState = State.NONE;
        itemsToUse = 0;
        originalSlot = -1;
        targetSlot = -1;
        healPerItem = 0;
    }
}