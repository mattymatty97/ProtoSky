package protosky.mixins.worldgen.below_zero;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BelowZeroRetrogen.class)
public class BelowZeroRetrogenMixin {
    @ModifyExpressionValue(method = "getBiomeResolver", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;isUpgrading()Z"))
    private static boolean useNormalBiomeSupplier(boolean original) {
        return false;
    }
}
