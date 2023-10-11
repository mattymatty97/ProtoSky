package protosky.mixins.worldgen.below_zero;

import net.minecraft.world.chunk.BelowZeroRetrogen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BelowZeroRetrogen.class)
public class BelowZeroRetrogenMixin {
/*
    @ModifyExpressionValue(method = "getBiomeSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;hasBelowZeroRetrogen()Z"))
    private static boolean useNormalBiomeSupplier(boolean original){
        return (true)?false:original;
    }
 */
}
