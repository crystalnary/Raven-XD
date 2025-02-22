package keystrokesmod.module.impl.combat.aimassist;

import keystrokesmod.event.render.Render2DEvent;
import keystrokesmod.eventbus.annotations.EventListener;
import keystrokesmod.mixins.impl.client.PlayerControllerMPAccessor;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NormalAssist extends SubMode<AimAssist> {
    public static ButtonSetting throughBlock, clickAim, aimPitch, weaponOnly, breakBlocks, blatantMode, ignoreTeammates, lockOn, ignoreInvis;
    public static SliderSetting speedYaw, complimentYaw, speedPitch, complimentPitch, distance, pitchOffSet, maxAngle;
    private EntityPlayer currentTarget = null;

    public NormalAssist(String name, AimAssist parent) {
        super(name, parent);
        this.registerSetting(clickAim = new ButtonSetting("Click aim", true));
        this.registerSetting(breakBlocks = new ButtonSetting("Break blocks", true));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(lockOn = new ButtonSetting("Lock on", false));
        this.registerSetting(blatantMode = new ButtonSetting("Blatant mode", false));
        this.registerSetting(aimPitch = new ButtonSetting("Aim pitch", false));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", false));
        this.registerSetting(throughBlock = new ButtonSetting("Through block", true));
        this.registerSetting(ignoreInvis = new ButtonSetting("Ignore Invis", false));
        this.registerSetting(speedYaw = new SliderSetting("Speed 1 (yaw)", 45.0D, 5.0D, 50.0D, 1.0D));
        this.registerSetting(maxAngle = new SliderSetting("Max angle", 180, 1, 360, 5));
        this.registerSetting(complimentYaw = new SliderSetting("Speed 2 (yaw)", 15.0D, 2D, 48.0D, 1.0D));
        this.registerSetting(speedPitch = new SliderSetting("Speed 1 (pitch)", 45.0D, 5.0D, 50.0D, 1.0D));
        this.registerSetting(complimentPitch = new SliderSetting("Speed 2 (pitch)", 15.0D, 2D, 48.0D, 1.0D));
        this.registerSetting(pitchOffSet = new SliderSetting("pitchOffSet (blocks)", 0.0D, -2, 2, 0.050D));
        this.registerSetting(distance = new SliderSetting("Distance", 5, 1, 8, 0.1));
    }

    @EventListener
    public void onRender(Render2DEvent event) {
        if (noAction()) {
            return;
        }
        final EntityPlayer target = getEnemy();
        if (target == null) return;
        if (blatantMode.isToggled()) {
            Utils.aim(target, (float) pitchOffSet.getInput());
        } else {
            double n = Utils.fovFromEntity(target);
            if ((n > 1.0D) || (n < -1.0D)) {
                double complimentSpeed = n * (ThreadLocalRandom.current().nextDouble(complimentYaw.getInput() - 1.47328, complimentYaw.getInput() + 2.48293) / 100);
                float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedYaw.getInput() - 4.723847, speedYaw.getInput())))));
                mc.thePlayer.rotationYaw += val;
            }
            if (aimPitch.isToggled()) {
                double complimentSpeed = Utils.PitchFromEntity(target, (float) pitchOffSet.getInput()) * (ThreadLocalRandom.current().nextDouble(complimentPitch.getInput() - 1.47328, complimentPitch.getInput() + 2.48293) / 100);
                float val = (float) (-(complimentSpeed + (n / (101.0D - (float) ThreadLocalRandom.current().nextDouble(speedPitch.getInput() - 4.723847, speedPitch.getInput())))));
                mc.thePlayer.rotationPitch += val;
            }
        }
    }

    private boolean noAction() {
        if (mc.currentScreen != null || !mc.inGameHasFocus) return true;
        if (weaponOnly.isToggled() && !Utils.holdingWeapon()) return true;
        if (clickAim.isToggled() && Utils.isNotLeftClicking()) return true;
        return breakBlocks.isToggled() && ((PlayerControllerMPAccessor) mc.playerController).isHittingBlock();
    }

    private @Nullable EntityPlayer getEnemy() {
        if (lockOn.isToggled() && currentTarget != null) {
            if (isValid(currentTarget)) {
                return currentTarget;
            } else {
                currentTarget = null;
            }
        } else {
            currentTarget = null;
        }

        final int fov = (int) maxAngle.getInput();
        final List<EntityPlayer> players = mc.theWorld.playerEntities;
        final Vec3 playerEyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        EntityPlayer target = null;
        double targetFov = Double.MAX_VALUE;
        for (final EntityPlayer entityPlayer : players) {
            if (entityPlayer != mc.thePlayer && entityPlayer.deathTime == 0) {
                Vec3 targetEyes = new Vec3(entityPlayer.posX, entityPlayer.posY + entityPlayer.getEyeHeight(), entityPlayer.posZ);
                double dist = playerEyes.distanceTo(targetEyes);
                if (ignoreInvis.isToggled() && entityPlayer.isInvisible())
                    continue;
                if (Utils.isFriended(entityPlayer))
                    continue;
                if (AntiBot.isBot(entityPlayer))
                    continue;
                if (ignoreTeammates.isToggled() && Utils.isTeamMate(entityPlayer))
                    continue;
                if (dist > distance.getInput())
                    continue;
                if (fov != 360 && !Utils.inFov(fov, entityPlayer))
                    continue;

                float[] rotations = RotationUtils.getRotations(targetEyes.x, targetEyes.y, targetEyes.z, playerEyes.toVec3());
                if (!throughBlock.isToggled() && RotationUtils.rayCast(dist, rotations[0], rotations[1]) != null)
                    continue;

                double curFov = Math.abs(Utils.getFov(entityPlayer.posX, entityPlayer.posZ));
                if (curFov < targetFov) {
                    target = entityPlayer;
                    targetFov = curFov;
                }
            }
        }

        if (lockOn.isToggled() && target != null) {
            currentTarget = target;
        }
        return target;
    }

    private boolean isValid(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime != 0) return false;

        Vec3 playerEyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 targetEyes = new Vec3(entityPlayer.posX, entityPlayer.posY + entityPlayer.getEyeHeight(), entityPlayer.posZ);
        double dist = playerEyes.distanceTo(targetEyes);

        if (dist > distance.getInput()) return false;
        if (Utils.isFriended(entityPlayer)) return false;
        if (AntiBot.isBot(entityPlayer)) return false;
        if (ignoreTeammates.isToggled() && Utils.isTeamMate(entityPlayer)) return false;
        if (ignoreInvis.isToggled() && entityPlayer.isInvisible()) return false;

        int currentFov = (int) maxAngle.getInput();
        if (currentFov != 360 && !Utils.inFov(currentFov, entityPlayer)) {
            return false;
        }

        if (!throughBlock.isToggled()) {
            float[] rotations = RotationUtils.getRotations(targetEyes.x, targetEyes.y, targetEyes.z, playerEyes.toVec3());
            return RotationUtils.rayCast(dist, rotations[0], rotations[1]) == null;
        }

        return true;
    }

    public void onDisable() {
        currentTarget = null;
    }
}
