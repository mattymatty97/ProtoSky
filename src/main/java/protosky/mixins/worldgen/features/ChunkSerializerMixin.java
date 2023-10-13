package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ProtoSkyMod;
import protosky.interfaces.GraceHolder;

import java.util.Map;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @ModifyReturnValue(method = "deserialize", at = @At("RETURN"))
    private static ProtoChunk deserialize_graces(ProtoChunk protoChunk, ServerWorld world, PointOfInterestStorage poiStorage, ChunkPos chunkPos, NbtCompound nbt){
        if (nbt.contains(ProtoSkyMod.GRACES_TAG)){
            NbtCompound graces_compound = nbt.getCompound(ProtoSkyMod.GRACES_TAG);

            GraceHolder graceHolder = (GraceHolder)protoChunk;

            NbtList gracedEntities = graces_compound.getList(GraceHolder.ENTITY_TAG, NbtElement.COMPOUND_TYPE);
            NbtList gracedBlockStates = graces_compound.getList(GraceHolder.BLOCKSTATE_TAG, NbtElement.COMPOUND_TYPE);

            gracedEntities.forEach((entity_nbt)->{
                graceHolder.protoSky$getGracedEntities().add((NbtCompound)entity_nbt);
            });

            gracedBlockStates.forEach(nbtElement -> {
                NbtCompound block_nbt = (NbtCompound)nbtElement;
                BlockPos pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, block_nbt.get("pos")).getOrThrow(false, ProtoSkyMod.LOGGER::error);
                BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, block_nbt.get("blockstate")).getOrThrow(false, ProtoSkyMod.LOGGER::error);
                graceHolder.protoSky$putGracedBlock(pos, state);
            });
        }
        return protoChunk;
    }

    @ModifyReturnValue(method = "serialize", at=@At("RETURN"))
    private static NbtCompound serialize_graces(NbtCompound nbt, ServerWorld world, Chunk chunk){
        //save the graces only if we haven't yet fully generated the chunk
        //free storage space
        if (!chunk.getStatus().isAtLeast(ChunkStatus.LIGHT)) {
            GraceHolder graceHolder = (GraceHolder) chunk;
            NbtList gracedEntities = new NbtList();
            gracedEntities.addAll(graceHolder.protoSky$getGracedEntities());

            NbtList gracedBlocks = new NbtList();

            for (Map.Entry<BlockPos, BlockState> entry : graceHolder.protoSky$getGracedBlocks().entrySet()) {
                NbtCompound block_nbt = new NbtCompound();
                block_nbt.put("pos", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.getKey()).getOrThrow(false, ProtoSkyMod.LOGGER::error));
                block_nbt.put("blockstate", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).getOrThrow(false, ProtoSkyMod.LOGGER::error));
                gracedBlocks.add(block_nbt);
            }

            NbtCompound protosky = new NbtCompound();
            protosky.put(GraceHolder.ENTITY_TAG, gracedEntities);
            protosky.put(GraceHolder.BLOCKSTATE_TAG, gracedBlocks);

            nbt.put(ProtoSkyMod.GRACES_TAG, protosky);
        }

        return nbt;
    }
}
