package keystrokesmod.module.impl.player.blink;

import keystrokesmod.Client;
import keystrokesmod.event.render.Render3DEvent;
import keystrokesmod.event.network.SendPacketEvent;
import keystrokesmod.eventbus.annotations.EventListener;
import keystrokesmod.module.impl.player.Blink;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.backtrack.TimedPacket;
import keystrokesmod.utility.render.Animation;
import keystrokesmod.utility.render.Easing;
import keystrokesmod.utility.render.progress.Progress;
import keystrokesmod.utility.render.progress.ProgressManager;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.status.client.C00PacketServerQuery;
import keystrokesmod.event.render.Render2DEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FakeLagBlink extends SubMode<Blink> {
    private final SliderSetting maxBlinkTime;
    private final ButtonSetting slowRelease;
    private final SliderSetting releaseSpeed;
    private final ButtonSetting antiAim;
    private final ButtonSetting drawRealPosition;

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Animation animationX = new Animation(Easing.EASE_OUT_CIRC, 200);
    private final Animation animationY = new Animation(Easing.EASE_OUT_CIRC, 200);
    private final Animation animationZ = new Animation(Easing.EASE_OUT_CIRC, 200);
    private final Progress progress = new Progress("Blink");
    public boolean needToDisable = false;
    private Vec3 vec3 = Vec3.ZERO;
    private long startTime = 0;
    private long stopTime = 0;
    private long blinkedTime = 0;

    public FakeLagBlink(String name, @NotNull Blink parent) {
        super(name, parent);
        this.registerSetting(maxBlinkTime = new SliderSetting("Max blink time", 20000, 1000, 30000, 500, "ms"));
        this.registerSetting(slowRelease = new ButtonSetting("Slow release", false));
        this.registerSetting(releaseSpeed = new SliderSetting("Release speed", 2, 2, 10, 0.1, "x", slowRelease::isToggled));
        this.registerSetting(antiAim = new ButtonSetting("Anti-Aim", true));
        this.registerSetting(drawRealPosition = new ButtonSetting("Draw real position", true));
    }

    @Override
    public void onEnable() throws Throwable {
        needToDisable = false;
        vec3 = new Vec3(mc.thePlayer);
        animationX.setValue(vec3.x());
        animationY.setValue(vec3.y());
        animationZ.setValue(vec3.z());
        startTime = System.currentTimeMillis();
        blinkedTime = 0;
        ProgressManager.add(progress);
    }

    @Override
    public void onDisable() throws Throwable {
        if (!needToDisable) {
            Client.EVENT_BUS.register(this);
            Client.EVENT_BUS.register(this);
            stopTime = System.currentTimeMillis();
            needToDisable = true;
        }
    }

    @Override
    public String getInfo() {
        return String.valueOf(packetQueue.size());
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        animationX.run(vec3.x());
        animationY.run(vec3.y());
        animationZ.run(vec3.z());
        if (drawRealPosition.isToggled()) {
            Blink.drawBox(new net.minecraft.util.Vec3(animationX.getValue(), animationY.getValue(), animationZ.getValue()));
        }
    }

    @EventListener(priority = 2)
    public void onRenderTick(Render2DEvent ev) {
        if (needToDisable) {
            progress.setProgress((blinkedTime / maxBlinkTime.getInput()) - Math.min((System.currentTimeMillis() - stopTime) / (maxBlinkTime.getInput() / releaseSpeed.getInput()), 1));
        } else {
            blinkedTime = Math.min(System.currentTimeMillis() - startTime, (long) maxBlinkTime.getInput());
            progress.setProgress(blinkedTime / maxBlinkTime.getInput());
        }

        if (!Utils.nullCheck()) {
            sendPacket(false);
            return;
        } else if (needToDisable) {
            synchronized (packetQueue) {
                sendPacket(false);
                if (packetQueue.isEmpty()) {
                    Client.EVENT_BUS.unregister(this);
                    Client.EVENT_BUS.unregister(this);
                    needToDisable = false;
                    ProgressManager.remove(progress);
                    return;
                }
            }
        }
        sendPacket(true);
    }

    @EventListener(priority = 2)
    public void onSendPacket(@NotNull SendPacketEvent e) {
        if (!Utils.nullCheck()) return;
        final Packet<?> packet = e.getPacket();
        if (packet instanceof C00Handshake
                || packet instanceof C00PacketLoginStart
                || packet instanceof C00PacketServerQuery
                || packet instanceof C01PacketEncryptionResponse
                || packet instanceof C01PacketChatMessage) {
            return;
        }
        if (e.isCancelled()) {
            return;
        }
        packetQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
        e.cancel();
    }

    public void sendPacket(boolean delay) {
        try {
            while (!packetQueue.isEmpty()) {
                boolean shouldSend;
                if (delay && !(antiAim.isToggled() && shouldAntiAim())) {
                    shouldSend = packetQueue.element().getCold().getCum((long) maxBlinkTime.getInput());
                } else {
                    if (slowRelease.isToggled()) {
                        shouldSend = packetQueue.element().getCold().getCum((long) (maxBlinkTime.getInput() * ((blinkedTime / maxBlinkTime.getInput()) - Math.min((System.currentTimeMillis() - stopTime) / (maxBlinkTime.getInput() / releaseSpeed.getInput()), 1))));
                    } else {
                        shouldSend = true;
                    }
                }

                if (shouldSend) {
                    Packet<?> packet = packetQueue.remove().getPacket();
                    if (packet == null) continue;

                    PacketUtils.getPos(packet).ifPresent(pos -> vec3 = pos);
                    PacketUtils.sendPacketNoEvent(packet);
                } else {
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean shouldAntiAim() {
        return mc.theWorld.playerEntities.parallelStream()
                .filter(target -> target != mc.thePlayer)
                .filter(target -> !AntiBot.isBot(target))
                .filter(target -> !Utils.isTeamMate(target))
                .filter(target -> new Vec3(target).distanceTo(vec3) < 5)
                .anyMatch(target -> Utils.inFov(target.getRotationYawHead(), 120, vec3.x(), vec3.z()));
    }
}
