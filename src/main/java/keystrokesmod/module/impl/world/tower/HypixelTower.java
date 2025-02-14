package keystrokesmod.module.impl.world.tower;

import keystrokesmod.Client;
import keystrokesmod.event.player.MoveEvent;
import keystrokesmod.event.player.PreUpdateEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.Tower;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.*;
import keystrokesmod.utility.movement.MoveCorrect;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import keystrokesmod.eventbus.annotations.EventListener;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HypixelTower extends SubMode<Tower> {
    public static final Set<EnumFacing> LIMIT_FACING = new HashSet<>(Collections.singleton(EnumFacing.SOUTH));
    public static final MoveCorrect moveCorrect = new MoveCorrect(0.3, MoveCorrect.Mode.POSITION);

    private final ButtonSetting notWhileMoving;
    private final SliderSetting stopOnBlocks;
    private final SliderSetting deSyncAmount;
    private boolean towering;
    private int towerTicks;
    private boolean blockPlaceRequest = false;
    private int lastOnGroundY;
    private BlockPos deltaPlace = BlockPos.ORIGIN;
    private int verticalPlaced = 0;

    public HypixelTower(String name, @NotNull Tower parent) {
        super(name, parent);
        this.registerSetting(notWhileMoving = new ButtonSetting("Not while moving", true));
        this.registerSetting(new DescriptionSetting("Vertical"));
        this.registerSetting(stopOnBlocks = new SliderSetting("Stop on blocks", 6, 6, 10, 1));
        this.registerSetting(deSyncAmount = new SliderSetting("De-sync amount", 50, 0, 300, 50, "ms"));
    }

    public static boolean isGoingDiagonally(double amount) {
        return Math.abs(mc.thePlayer.motionX) > amount && Math.abs(mc.thePlayer.motionZ) > amount;
    }

    public static double randomAmount() {
        return 8.0E-4 + Math.random() * 0.008;
    }

    @EventListener
    public void onMove(MoveEvent event) throws IllegalAccessException {
        if (mc.thePlayer.isPotionActive(Potion.jump)) return;
        final boolean airUnder = !BlockUtils.insideBlock(
                mc.thePlayer.getEntityBoundingBox()
                        .offset(0, -1, 0)
                        .expand(0.3, 0, 0.3)
        );

        if (!MoveUtil.isMoving() && parent.canTower()) {
            if (!moveCorrect.isDoneZ()) {
                if (mc.thePlayer.posY - lastOnGroundY < 1) return;

                MoveUtil.stop();
                if (!moveCorrect.moveZ(true))
                    return;
            }

            blockPlaceRequest = true;
        }

        if ((MoveUtil.speed() > 0.1 && !notWhileMoving.isToggled()) || !MoveUtil.isMoving()) {
            double towerSpeed = isGoingDiagonally(0.1) ? 0.22 : 0.29888888;
            if (!mc.thePlayer.onGround) {
                if (this.towering) {
                    if (this.towerTicks == 2) {
                        event.setY(Math.floor(mc.thePlayer.posY + 1.0) - mc.thePlayer.posY);
                    } else if (this.towerTicks == 3) {
                        if (parent.canTower()) {
                            event.setY(mc.thePlayer.motionY = 0.4198499917984009);
                            if (MoveUtil.isMoving()) {
                                MoveUtil.strafe((float) towerSpeed - randomAmount());
                            }
                            this.towerTicks = 0;
                        } else {
                            this.towering = false;
                        }
                    }
                }
            } else {
                this.towering = parent.canTower() && !airUnder;
                if (this.towering) {
                    this.towerTicks = 0;
                    if (event.getY() > 0.0) {
                        event.setY(mc.thePlayer.motionY = 0.4198479950428009);
                        if (MoveUtil.isMoving()) {
                            MoveUtil.strafe((float) towerSpeed - randomAmount());
                        }
                    }
                }
            }

            ++this.towerTicks;
        }
    }

    @EventListener(priority = -1)
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer.onGround) {
            lastOnGroundY = (int) mc.thePlayer.posY;
            deltaPlace = new BlockPos(0, 1, 1);
        }

        if (blockPlaceRequest && !Utils.isMoving()) {
            if (verticalPlaced >= stopOnBlocks.getInput() || mc.thePlayer.onGround) {
                towering = false;
                blockPlaceRequest = false;
                verticalPlaced = 0;
                return;
            }

            MovingObjectPosition lastScaffoldPlace = ModuleManager.scaffold.placeBlock;
            if (lastScaffoldPlace == null)
                return;
            Optional<Triple<BlockPos, EnumFacing, Vec3>> optionalPlaceSide = RotationUtils.getPlaceSide(
                    lastScaffoldPlace.getBlockPos().add(deltaPlace),
                    LIMIT_FACING
            );
            if (!optionalPlaceSide.isPresent())
                return;

            Triple<BlockPos, EnumFacing, Vec3> placeSide = optionalPlaceSide.get();

            Client.getExecutor().schedule(() -> {
                if (ModuleManager.scaffold.place(
                        new MovingObjectPosition(placeSide.getRight().toVec3(), placeSide.getMiddle(), placeSide.getLeft()),
                        false
                )) {
                    verticalPlaced++;
                }
            }, (int) deSyncAmount.getInput(), TimeUnit.MILLISECONDS);
            blockPlaceRequest = false;
        } else {
            verticalPlaced = 0;
        }
    }

    @Override
    public void onEnable() throws Throwable {
        verticalPlaced = 0;
    }
}
