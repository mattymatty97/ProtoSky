package protosky.mixins.height_fix;

import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import protosky.interfaces.RetrogenHolder;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin {

    @Inject(method = "setBelowZeroRetrogen", at = @At("HEAD"))
    private void onBelowZeroRetrogen(BelowZeroRetrogen belowZeroRetrogen, CallbackInfo ci){
        if (belowZeroRetrogen != null)
            ((RetrogenHolder)this).setBelowZeroRetrogen(true);
    }
}
