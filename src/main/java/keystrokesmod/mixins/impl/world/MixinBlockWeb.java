package keystrokesmod.mixins.impl.world;


import keystrokesmod.Client;
import keystrokesmod.event.world.BlockWebEvent;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockWeb.class)
public class MixinBlockWeb {

    @Inject(method = "onEntityCollidedWithBlock", at = @At("HEAD"), cancellable = true)
    public void onEntityCollidedWithBlock(World world, BlockPos blockPos, IBlockState state, Entity entity, CallbackInfo ci) {
        if (Utils.nullCheck() && entity == Client.mc.thePlayer) {
            BlockWebEvent event = new BlockWebEvent(blockPos, state);
            Client.EVENT_BUS.post(event);

            if (event.isCancelled())
                ci.cancel();
        }
    }
}
