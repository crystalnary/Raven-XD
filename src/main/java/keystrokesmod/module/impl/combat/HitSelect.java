package keystrokesmod.module.impl.combat;

import keystrokesmod.eventbus.annotations.EventListener;
import keystrokesmod.event.player.PreMotionEvent;
import keystrokesmod.event.client.PreTickEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import net.minecraft.client.Minecraft;

public class HitSelect extends Module {
    private enum State {
        WAITING_FOR_HIT,
        COOLDOWN,
        ATTACKING
    }

    private State currentState = State.WAITING_FOR_HIT;
    private int cooldownTicks = 0;
    private int prevHurtTime = 0;

    public HitSelect() {
        super("HitSelect", Module.category.combat);
        this.registerSetting(new DescriptionSetting("Automates hit-selecting for PvP advantage."));
    }

    @Override
    public void onEnable() {
        currentState = State.WAITING_FOR_HIT;
        cooldownTicks = 0;
        prevHurtTime = 0;
    }

    @Override
    public void onDisable() {
        currentState = State.WAITING_FOR_HIT;
    }

    @EventListener
    public void onPreTick(PreTickEvent e) {
        int currentHurtTime = Minecraft.getMinecraft().thePlayer.hurtTime;

        if (currentState == State.WAITING_FOR_HIT) {
            if (currentHurtTime == 10 && prevHurtTime < 10) {
                currentState = State.COOLDOWN;
                cooldownTicks = 2;
            }
        } else if (currentState == State.COOLDOWN) {
            if (cooldownTicks > 0) {
                cooldownTicks--;
            } else {
                currentState = State.ATTACKING;
            }
        } else if (currentState == State.ATTACKING && cooldownTicks > 0) {
            cooldownTicks--;
        }

        prevHurtTime = currentHurtTime;
    }

    public static boolean canAttack() {
        if (ModuleManager.hitSelect == null || !ModuleManager.hitSelect.isEnabled()) return true;
        return ModuleManager.hitSelect.currentState == State.ATTACKING &&
                ModuleManager.hitSelect.cooldownTicks <= 0;
    }

    public static void registerAttack() {
        if (ModuleManager.hitSelect != null && ModuleManager.hitSelect.isEnabled()) {
            ModuleManager.hitSelect.cooldownTicks = 2 + (int)(Math.random() * 2);
        }
    }
}
