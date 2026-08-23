package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ProtoSkyMod;
import protosky.interfaces.GraceHolder;

import java.util.Map;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @ModifyReturnValue(method = "read", at = @At("RETURN"))
    private static ProtoChunk deserialize_graces(ProtoChunk protoChunk, @Local(argsOnly = true, name = "compoundTag") CompoundTag nbt) {
        if (nbt.contains(ProtoSkyMod.GRACES_TAG)) {
            CompoundTag graces_compound = nbt.getCompound(ProtoSkyMod.GRACES_TAG);

            GraceHolder graceHolder = (GraceHolder) protoChunk;

            ListTag gracedEntities = graces_compound.getList(GraceHolder.ENTITY_TAG, Tag.TAG_COMPOUND);
            ListTag gracedBlockStates = graces_compound.getList(GraceHolder.BLOCKSTATE_TAG, Tag.TAG_COMPOUND);

            gracedEntities.forEach((entity_nbt) -> {
                graceHolder.protoSky$getGracedEntities().add((CompoundTag) entity_nbt);
            });

            gracedBlockStates.forEach(nbtElement -> {
                CompoundTag block_nbt = (CompoundTag) nbtElement;
                BlockPos pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, block_nbt.get("pos")).getOrThrow(false, ProtoSkyMod.LOGGER::error);
                BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, block_nbt.get("blockstate")).getOrThrow(false, ProtoSkyMod.LOGGER::error);
                graceHolder.protoSky$putGracedBlock(pos, state);
            });
        }
        return protoChunk;
    }

    @ModifyReturnValue(method = "write", at = @At("RETURN"))
    private static CompoundTag serialize_graces(CompoundTag nbt, @Local(argsOnly = true, name = "chunkAccess") ChunkAccess chunk) {
        //save the graces only if we haven't yet fully generated the chunk
        //free storage space
        if (!chunk.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
            GraceHolder graceHolder = (GraceHolder) chunk;
            ListTag gracedEntities = new ListTag();
            gracedEntities.addAll(graceHolder.protoSky$getGracedEntities());

            ListTag gracedBlocks = new ListTag();

            for (Map.Entry<BlockPos, BlockState> entry : graceHolder.protoSky$getGracedBlocks().entrySet()) {
                CompoundTag block_nbt = new CompoundTag();
                block_nbt.put("pos", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.getKey()).getOrThrow(false, ProtoSkyMod.LOGGER::error));
                block_nbt.put("blockstate", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow(false, ProtoSkyMod.LOGGER::error));
                gracedBlocks.add(block_nbt);
            }

            CompoundTag protosky = new CompoundTag();
            protosky.put(GraceHolder.ENTITY_TAG, gracedEntities);
            protosky.put(GraceHolder.BLOCKSTATE_TAG, gracedBlocks);

            nbt.put(ProtoSkyMod.GRACES_TAG, protosky);
        }

        return nbt;
    }
}
