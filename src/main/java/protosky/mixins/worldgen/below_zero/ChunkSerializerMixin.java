package protosky.mixins.worldgen.below_zero;


import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.interfaces.RetrogenHolder;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ProtoChunk;setStatus(Lnet/minecraft/world/level/chunk/ChunkStatus;)V", shift = At.Shift.BEFORE))
    private static void deserialize_datafix_status(
            CallbackInfoReturnable<ProtoChunk> cir,
            @Local(argsOnly = true, name = "compoundTag") CompoundTag nbt,
            @Local(name = "protoChunk") ProtoChunk protoChunk
    ) {
        if (nbt.contains(ProtoSkyMod.OLD_STATUS_TAG)) {
            try {
                ChunkStatus old_status = ChunkStatus.byName(nbt.getString("protosky_old_status"));
                ((RetrogenHolder) protoChunk).protoSky$setPreviousStatus(old_status);
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Exception while loading old Chunk: ", t);
            }
        }
    }


    @ModifyReturnValue(method = "write", at = @At("RETURN"))
    private static CompoundTag serialize_datafix_status(CompoundTag nbt, @Local(argsOnly = true, name = "chunkAccess") ChunkAccess chunk) {
        if (!chunk.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
            if (((RetrogenHolder) chunk).protoSky$getPreviousStatus() != ChunkStatus.EMPTY) {
                nbt.putString(ProtoSkyMod.OLD_STATUS_TAG, ((RetrogenHolder) chunk).protoSky$getPreviousStatus().toString());
            }
        }
        return nbt;
    }
}
