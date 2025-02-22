package keystrokesmod.module.impl.combat.aimassist;

import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;

public class AdaptiveAssist extends SubMode<AimAssist> {
    public AdaptiveAssist(String name, AimAssist parent) {
        super(name, parent);
    }

    @Override
    public void onEnable() {
        Utils.sendMessage("AdaptiveAssist enabled");
    }

    @Override
    public void onDisable() {
        Utils.sendMessage("AdaptiveAssist disabled");
    }
}
