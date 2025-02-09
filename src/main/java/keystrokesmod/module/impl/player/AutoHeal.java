package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ContainerUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import keystrokesmod.eventbus.annotations.EventListener;
import keystrokesmod.event.render.Render2DEvent;

public class AutoHeal extends Module {
    private final ButtonSetting autoDrop;
    private final SliderSetting minHealth;
    private final SliderSetting healDelay;
    private final SliderSetting startDelay;
    private final SliderSetting useDelay;
    private final ButtonSetting goldenHead;
    private final ButtonSetting soup;
    private final ButtonSetting goldenHeadName;
    private final ButtonSetting noInventory;
    private long lastHeal = -1;
    private long lastSwitchTo = -1;
    private int originalSlot = -1;

    private enum State {
        NONE,
        SWITCHING,
        USING,
        SWITCHING_BACK
    }

    private State currentState = State.NONE;

    public AutoHeal() {
        super("AutoHeal", category.player);
        this.registerSetting(new DescriptionSetting("Automatically uses healing items."));
        this.registerSetting(goldenHead = new ButtonSetting("Golden head", true));
        this.registerSetting(goldenHeadName = new ButtonSetting("Check golden Head name", true, goldenHead::isToggled));
        this.registerSetting(soup = new ButtonSetting("Soup", false));
        this.registerSetting(noInventory = new ButtonSetting("Disable in inventory", false));
        this.registerSetting(autoDrop = new ButtonSetting("Auto drop", false, soup::isToggled));
        this.registerSetting(minHealth = new SliderSetting("Min health", 10, 0, 20, 1));
        this.registerSetting(healDelay = new SliderSetting("Heal delay", 500, 0, 8500, 1));
        this.registerSetting(startDelay = new SliderSetting("Start delay", 0, 0, 300, 1));
        this.registerSetting(useDelay = new SliderSetting("Use delay", 250, 0, 1000, 1));
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (!Utils.nullCheck() || mc.thePlayer.isDead || mc.playerController == null) return;
        if (System.currentTimeMillis() - lastHeal < healDelay.getInput()) return;
        if (noInventory.isToggled() && mc.currentScreen instanceof GuiInventory) return;


        switch (currentState) {
            case NONE:
                if (mc.thePlayer.getHealth() <= minHealth.getInput()) {
                    int toSlot = -1;

                    if (goldenHead.isToggled()) {
                        for (int slot = 0; slot <= 8; slot++) {
                            ItemStack itemInSlot = mc.thePlayer.inventory.getStackInSlot(slot);
                            if (itemInSlot != null && itemInSlot.getItem() instanceof ItemSkull) {

                                if (goldenHeadName.isToggled()) {
                                    String displayName = itemInSlot.getDisplayName().toLowerCase();
                                    if ((displayName.contains("golden") && displayName.contains("head"))) {
                                        toSlot = slot;
                                        break;
                                    }
                                } else {
                                    toSlot = slot;
                                    break;
                                }
                            }
                        }
                    }

                    if (toSlot == -1 && soup.isToggled()) {
                        toSlot = ContainerUtils.getSlot(ItemSoup.class);
                    }

                    if (toSlot != -1) {
                        originalSlot = mc.thePlayer.inventory.currentItem;
                        SlotHandler.setCurrentSlot(toSlot);
                        lastSwitchTo = System.currentTimeMillis();
                        currentState = State.SWITCHING;
                    }
                }
                break;

            case SWITCHING:
                if (System.currentTimeMillis() - lastSwitchTo >= startDelay.getInput()) {
                    ItemStack stack = SlotHandler.getHeldItem();
                    if (stack != null) {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, stack);
                        lastSwitchTo = System.currentTimeMillis(); // Update for use duration check
                        currentState = State.USING;
                    } else {
                        // Item wasn't found, reset.
                        currentState = State.NONE;
                        lastHeal = System.currentTimeMillis(); // Prevent rapid retries
                    }
                }
                break;

            case USING:
                ItemStack usingStack = SlotHandler.getHeldItem();
                if (usingStack == null) {
                    currentState = State.NONE;
                    lastHeal = System.currentTimeMillis();
                    break;
                }

                long itemUseDuration = usingStack.getItem().getMaxItemUseDuration(usingStack);
                long totalDelay = (long) ((itemUseDuration * 50L) + useDelay.getInput());

                if (System.currentTimeMillis() - lastSwitchTo >= totalDelay) {
                    if (autoDrop.isToggled() && usingStack.getItem() instanceof ItemSoup) {
                        mc.thePlayer.dropOneItem(true);
                    }
                    currentState = State.SWITCHING_BACK;
                }
                break;
            case SWITCHING_BACK:
                if (originalSlot != -1) {
                    SlotHandler.setCurrentSlot(originalSlot);
                    originalSlot = -1;
                }
                currentState = State.NONE;
                lastHeal = System.currentTimeMillis();
                break;
        }
    }
}