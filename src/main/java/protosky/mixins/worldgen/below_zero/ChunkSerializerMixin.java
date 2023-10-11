package protosky.mixins.worldgen.below_zero;


import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.interfaces.RetrogenHolder;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
    @Inject(method = "deserialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ProtoChunk;setStatus(Lnet/minecraft/world/chunk/ChunkStatus;)V", shift = At.Shift.BEFORE))
    private static void fix_incomplete(ServerWorld world, PointOfInterestStorage poiStorage, ChunkPos chunkPos, NbtCompound nbt, CallbackInfoReturnable<ProtoChunk> cir, @Local ProtoChunk protoChunk){
        if (nbt.contains(ProtoSkyMod.OLD_STATUS_TAG)){
            try {
                ChunkStatus old_status = ChunkStatus.byId(nbt.getString("protosky_old_status"));
                ((RetrogenHolder) protoChunk).protoSky$setPreviousStatus(old_status);
            }catch (Throwable t){
                ProtoSkyMod.LOGGER.error("Exception while loading old Chunk: ", t);
            }
        }
    }


    @ModifyReturnValue(method = "serialize", at=@At("RETURN"))
    private static NbtCompound save_graces(NbtCompound nbt, ServerWorld world, Chunk chunk){
        if (((RetrogenHolder) chunk).protoSky$getPreviousStatus() != ChunkStatus.EMPTY){
            nbt.putString(ProtoSkyMod.OLD_STATUS_TAG,((RetrogenHolder) chunk).protoSky$getPreviousStatus().toString());
        }
        return nbt;
    }
}
