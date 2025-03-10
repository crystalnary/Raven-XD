package keystrokesmod.module.impl.movement.speed;

import keystrokesmod.event.player.PreMotionEvent;
import keystrokesmod.module.impl.client.Notifications;
import keystrokesmod.module.impl.movement.Speed;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import keystrokesmod.eventbus.annotations.EventListener;
import org.jetbrains.annotations.NotNull;

public class VanillaSpeed extends SubMode<Speed> {
    private static boolean warning = false;
    private final SliderSetting horizonSpeed;
    private final ButtonSetting autoJump;
    private final SliderSetting verticalSpeed;
    private final ButtonSetting fastStop;

    public VanillaSpeed(String name, @NotNull Speed parent) {
        super(name, parent);
        this.registerSetting(horizonSpeed = new SliderSetting("Horizon speed", 1, 0.1, 5, 0.1));
        this.registerSetting(autoJump = new ButtonSetting("Auto jump", true));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical speed", 1, 0.1, 3, 0.1, autoJump::isToggled));
        this.registerSetting(fastStop = new ButtonSetting("Fast stop", false));
    }

    @Override
    public void onEnable() {
        if (Utils.isHypixel() && !warning) {
            warning = true;
            Notifications.sendNotification(Notifications.NotificationTypes.WARN, "Vanilla Speed on Hypixel may result a ban!");
            parent.disable();
        }
    }

    @EventListener
    public void onPreMotion(PreMotionEvent event) {
        if (parent.noAction()) return;

        if (MoveUtil.isMoving()) {
            if (autoJump.isToggled() && mc.thePlayer.onGround) {
                MoveUtil.strafe();
                MoveUtil.jump();
                mc.thePlayer.motionY = verticalSpeed.getInput() * MoveUtil.jumpMotion();
            } else {
                MoveUtil.strafe(horizonSpeed.getInput() * MoveUtil.getAllowedHorizontalDistance());
            }
        } else {
            if (fastStop.isToggled())
                MoveUtil.stop();
        }
    }
}
