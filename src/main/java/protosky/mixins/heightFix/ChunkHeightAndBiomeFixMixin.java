package protosky.mixins.heightFix;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ChunkHeightAndBiomeFix;
import net.minecraft.datafixer.fix.ProtoChunkTickListFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(ChunkHeightAndBiomeFix.class)
public class ChunkHeightAndBiomeFixMixin {

    @ModifyVariable(method = "fixLevel", at=@At("HEAD"), ordinal = 0, argsOnly = true)
    private static Dynamic<?> storeOldStatus(Dynamic<?> level){
        return level.set("protosky_old_status", level.get("Status").result().orElseGet(()->((Dynamic)level.createString("empty"))));
    }

    @WrapOperation(method = "fixLevel", at = @At(value = "INVOKE",target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z", ordinal = 0), slice = @Slice(
            from = @At(value = "INVOKE", target = "Ljava/util/BitSet;<init>(I)V")
    ))
    private static boolean forceBelowZero2(String instance, Object other, Operation<Boolean> original){
        //TODO: add use config
        return true;
    }
    @WrapWithCondition(method = "fixLevel", at = @At(value = "INVOKE",target = "Ljava/util/BitSet;set(I)V"))
    private static boolean forceBelowZero(BitSet instance, int index){
        //TODO: add use config
        return false;
    }


}
