package protosky.mixins;

import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.interfaces.MutabilityHolder;

@Mixin(Heightmap.class)
public class HeightmapMixin {
    @Inject(method = "trackUpdate", at = @At("HEAD"), cancellable = true)
    private void trackUpdate(CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof MutabilityHolder holder && !holder.protoSky$isMutable())
            cir.setReturnValue(false);
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void set(CallbackInfo ci) {
        if (this instanceof MutabilityHolder holder && !holder.protoSky$isMutable())
            ci.cancel();
    }
}
