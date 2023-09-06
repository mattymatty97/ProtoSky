package protosky.mixins.height_fix;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.datafixer.fix.ChunkHeightAndBiomeFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.BitSet;

@Mixin(ChunkHeightAndBiomeFix.class)
public class ChunkHeightAndBiomeFixMixin {

    @WrapOperation(method = "fixLevel", at = @At(value = "INVOKE",target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z", ordinal = 0), slice = @Slice(
            from = @At(value = "INVOKE", target = "Ljava/util/BitSet;<init>(I)V")
    ))
    private static boolean forceBelowZero2(String instance, Object other, Operation<Boolean> original){
        //TODO: add use config
        return (true)? true: original.call(other);
    }

    @WrapWithCondition(method = "fixLevel", at = @At(value = "INVOKE",target = "Ljava/util/BitSet;set(I)V"))
    private static boolean forceBelowZero(BitSet instance, int index){
        //TODO: add use config
        return !true;
    }


}
