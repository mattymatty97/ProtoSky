package protosky.mixins.worldgen.below_zero;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ChunkHeightAndBiomeFix;
import net.minecraft.datafixer.fix.ProtoChunkTickListFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;

import java.util.BitSet;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.LongStream;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(ChunkHeightAndBiomeFix.class)
public class ChunkHeightAndBiomeFixMixin {

    @ModifyVariable(method = "fixLevel", at=@At("HEAD"), ordinal = 0, argsOnly = true)
    private static Dynamic<?> storeOldStatus(Dynamic<?> level){
        return level.set(ProtoSkyMod.OLD_STATUS_TAG, level.get("Status").result().orElseGet(()->((Dynamic)level.createString("empty"))));
    }

    @WrapOperation(method = "fixLevel", at = @At(value = "INVOKE",target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z", ordinal = 0), slice = @Slice(
            from = @At(value = "INVOKE", target = "Ljava/util/BitSet;<init>(I)V")
    ))
    private static boolean forceBelowZero2(String instance, Object other, Operation<Boolean> original){
        //force the datafix to run as if bedrock was there
        return true;
    }
    @WrapWithCondition(method = "fixLevel", at = @At(value = "INVOKE",target = "Ljava/util/BitSet;set(I)V"))
    private static boolean forceBelowZero(BitSet instance, int index){
        //prevent the list of air blocks from being populated ( make the datafix believe it is all bedrock )
        return false;
    }

    @Inject(method = "fixLevel", at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;", shift = At.Shift.AFTER), cancellable = true)
    private static void onEmptyChunk(Dynamic<?> level, boolean overworld, boolean heightAlreadyUpdated, boolean atNoiseStatus, Supplier<ProtoChunkTickListFix.class_6741> supplier, CallbackInfoReturnable<Dynamic<?>> cir, @Local Optional<? extends Dynamic<?>> optional){
        // and empty chunk will skip the datafix check entirely, so I had to replicate part of the vanilla code here to let empty chunks get datafixed too
        if (supplier.get() == null && optional.isPresent()){
            Dynamic<?> dynamic3 = "full".equals(optional.get().asString("")) ? level.createString("heightmaps") : optional.get();
            level = level.set(
                    "below_zero_retrogen",
                    level.createMap(
                            ImmutableMap.of(
                                    level.createString("target_status"), dynamic3, level.createString("missing_bedrock"), level.createLongList(LongStream.of(new BitSet(256).toLongArray()))
                            )
                    )
            );

            level = level.set("Status", level.createString("empty"));

            level = level.set("isLightOn", level.createBoolean(false));

            cir.setReturnValue(level);
        }
    }
}
