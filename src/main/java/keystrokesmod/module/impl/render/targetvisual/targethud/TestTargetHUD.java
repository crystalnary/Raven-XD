package keystrokesmod.module.impl.render.targetvisual.targethud;

import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.impl.render.targetvisual.ITargetVisual;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.IFont;
import keystrokesmod.utility.render.Animation;
import keystrokesmod.utility.render.Easing;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static keystrokesmod.module.impl.render.TargetHUD.*;

public class TestTargetHUD extends SubMode<TargetHUD> implements ITargetVisual {
    private final ModeSetting theme;
    private final ModeSetting font;
    private final ButtonSetting showStatus;
    private final ButtonSetting healthColor;
    private final Animation healthBarAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 240);
    private final Animation backgroundWidthAnimation = new Animation(Easing.EASE_IN_QUAD, 80);
    private final Animation playerXAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 80);
    private final Animation playerYAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 80);

    public TestTargetHUD(String name, @NotNull TargetHUD parent) {
        super(name, parent);
        this.registerSetting(theme = new ModeSetting("Theme", Theme.themes, 0));
        this.registerSetting(font = new ModeSetting("Font", new String[]{"Minecraft", "ProductSans", "Regular"}, 0));
        this.registerSetting(showStatus = new ButtonSetting("Show win or loss", true));
        this.registerSetting(healthColor = new ButtonSetting("Traditional health color", true));
    }

    private IFont getFont() {
        switch ((int) font.getInput()) {
            default:
            case 0:
                return FontManager.getMinecraft();
            case 1:
                return FontManager.productSansMedium;
            case 2:
                return FontManager.regular22;
        }
    }

    @Override
    public void render(@NotNull EntityLivingBase target) {
        String string = target.getDisplayName().getFormattedText();
        float health = Utils.limit(target.getHealth() / target.getMaxHealth(), 0, 1);
        string = string + " §a" + Math.round(target.getHealth()) + " §c❤ ";
        if (showStatus.isToggled() && mc.thePlayer != null) {
            String status = (health <= Utils.getCompleteHealth(mc.thePlayer) / mc.thePlayer.getMaxHealth()) ? "§aW" : "§cL";
            string = string + status;
        }

        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        final int n2 = 8;
        final int n3 = mc.fontRendererObj.getStringWidth(string) + n2 + 30;
        final int n4 = scaledResolution.getScaledWidth() / 2 - n3 / 2 + posX;
        final int n5 = scaledResolution.getScaledHeight() / 2 + 15 + posY;
        minX = n4 - n2;
        minY = n5 - n2;
        maxX = n4 + n3;
        maxY = n5 + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + n2;

        final int n10 = 255;
        final int n11 = Math.min(n10, 110);
        final int n12 = Math.min(n10, 210);

        backgroundWidthAnimation.run(maxX - minX);
        float animatedWidth = (float) backgroundWidthAnimation.getValue();
        float halfAnimatedWidth = animatedWidth / 2;
        float animatedMinX = (float) (minX + maxX) / 2 - halfAnimatedWidth;
        float animatedMaxX = (float) (minX + maxX) / 2 + halfAnimatedWidth;

        RenderUtils.drawRoundedRectangle(animatedMinX, (float) minY, animatedMaxX, (float) (maxY + 13), 10.0f, Utils.merge(Color.black.getRGB(), n11));

        final int n13 = minX + 6 + 30;
        final int n14 = maxX - 6;
        final int n15 = maxY;

        RenderUtils.drawRoundedRectangle((float) n13, (float) n15, (float) n14, (float) (n15 + 5), 4.0f, Utils.merge(Color.black.getRGB(), n11));

        float healthBar = (float) (int) (n14 + (n13 - n14) * (1.0 - ((health < 0.05) ? 0.05 : health)));
        if (healthBar - n13 < 3) {
            healthBar = n13 + 3;
        }

        healthBarAnimation.run(healthBar);
        float lastHealthBar = (float) healthBarAnimation.getValue();

        RenderUtils.drawRoundedGradientRect((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f,
                Utils.merge(Theme.getGradients((int) theme.getInput())[0], n12), Utils.merge(Theme.getGradients((int) theme.getInput())[0], n12),
                Utils.merge(Theme.getGradients((int) theme.getInput())[1], n12), Utils.merge(Theme.getGradients((int) theme.getInput())[1], n12));

        if (healthColor.isToggled()) {
            int healthTextColor = Utils.getColorForHealth(health);
            RenderUtils.drawRoundedRectangle((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f, healthTextColor);
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        getFont().drawString(string, (float) (n4 + 30), (float) n5, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | Utils.clamp(n10 + 15) << 24, true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        if (target instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer) target;
            double targetX = minX + 5;
            double targetY = minY + 4;
            playerXAnimation.run(targetX);
            playerYAnimation.run(targetY);
            double animatedX = playerXAnimation.getValue();
            double animatedY = playerYAnimation.getValue();
            double offset = -(player.hurtTime * 10);
            Color dynamicColor = new Color(255, (int) (255 + offset), (int) (255 + offset));
            GlStateManager.color(dynamicColor.getRed() / 255F, dynamicColor.getGreen() / 255F, dynamicColor.getBlue() / 255F, dynamicColor.getAlpha() / 255F);
            RenderUtils.renderPlayer2D((float) animatedX, (float) animatedY, 25, 25, player);
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}