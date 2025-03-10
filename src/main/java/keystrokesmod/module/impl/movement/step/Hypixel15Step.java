package keystrokesmod.module.impl.movement.step;

import keystrokesmod.event.player.PreMotionEvent;
import keystrokesmod.event.player.PreUpdateEvent;
import keystrokesmod.module.impl.movement.Step;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import keystrokesmod.utility.Utils;
import keystrokesmod.eventbus.annotations.EventListener;

public class Hypixel15Step extends SubMode<Step> {
    private final SliderSetting delay = new SliderSetting("Delay", 1000, 0, 5000, 250, "ms");
    private final ButtonSetting test = new ButtonSetting("Test", false);

    private int offGroundTicks = -1;
    private boolean stepping = false;
    private long lastStep = -1;

    public Hypixel15Step(String name, Step parent) {
        super(name, parent);
        this.registerSetting(delay, test);
    }

    @Override
    public void onDisable() {
        offGroundTicks = -1;
        stepping = false;
    }

    @EventListener
    public void onPreMotion(PreMotionEvent event) {
        final long time = System.currentTimeMillis();
        if (mc.thePlayer.onGround && mc.thePlayer.isCollidedHorizontally && MoveUtil.isMoving() && time - lastStep >= delay.getInput()) {
            stepping = true;
            lastStep = time;
        }
    }

    @EventListener
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else if (offGroundTicks != -1) {
            offGroundTicks++;
        }

        if (stepping) {
            if (!MoveUtil.isMoving() || Utils.jumpDown() || !mc.thePlayer.isCollidedHorizontally) {
                stepping = false;
                return;
            }

            switch (test.isToggled() ? offGroundTicks % 16 : offGroundTicks) {
                case 0:
                    MoveUtil.stop();
                    MoveUtil.strafe();
                    MoveUtil.jump();
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    MoveUtil.stop();
                    break;
                case 10:
                case 11:
                case 13:
                case 14:
                case 15:
                    mc.thePlayer.motionY = 0;
                    MoveUtil.stop();
                    break;
                case 16:
                    mc.thePlayer.motionY = .42;
                    stepping = false;
                    break;
            }
        }
    }
}
