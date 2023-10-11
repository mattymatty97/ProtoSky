package protosky.mixins.worldgen.below_zero;

import net.minecraft.world.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin {
/*
    @Inject(method = "setBelowZeroRetrogen", at = @At("HEAD"))
    private void onBelowZeroRetrogen(BelowZeroRetrogen belowZeroRetrogen, CallbackInfo ci){
        if (belowZeroRetrogen != null)
            ((RetrogenHolder)this).protoSky$setBelowZeroRetrogen(true);
    }
    */
}
