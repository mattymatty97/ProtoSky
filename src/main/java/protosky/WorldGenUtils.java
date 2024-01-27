package protosky;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import protosky.interfaces.GraceHolder;
import protosky.interfaces.RetrogenHolder;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldGenUtils
{

    /**
     * Empty all chunkSection of the provided chunk.
     * Skip sections above y0 if this was a datafixed Chunk
     * @param chunk the chunk to clear
     * @param world the world the chunk belongs to
     */
    public static void deleteBlocks(Chunk chunk, ServerWorld world) {
        //fixes for RetroGen
        ChunkStatus old_status = ((RetrogenHolder)chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isAtLeast(ChunkStatus.INITIALIZE_LIGHT);
        //This loops through all sections (16x16x16) sections of a chunk and copies over the biome information, but not the blocks.
        ChunkSection[] sections = chunk.getSectionArray();
        Map<BlockPos, BlockState> gracedBlocks = ((GraceHolder)chunk).protoSky$getGracedBlocks();
        for (int i = 0; i < sections.length; i++) {
            //avoid deleting blocks from pre 1.18 chunks
            if (had_retrogen && BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(chunk.sectionIndexToCoord(i)))
                continue;

            //clear the chunk
            ChunkSection chunkSection = sections[i];
            PalettedContainer<BlockState> blockStateContainer = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);

            ReadableContainer<RegistryEntry<Biome>> biomeContainer = chunkSection.getBiomeContainer();
            sections[i] = new ChunkSection(blockStateContainer, biomeContainer);
        }

        //This removes all the block entities
        for (BlockPos bePos : chunk.getBlockEntityPositions()) {
            //avoid deleting blocks from pre 1.18 chunks
            if (had_retrogen && BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(bePos.getY()))
                continue;
            //avoid deleting blockEntities that have been graced
            if (!gracedBlocks.containsKey(bePos))
                chunk.removeBlockEntity(bePos);
        }

    }

    public static final Set<Heightmap.Type> ALL_HEIGHTMAPS = Arrays.stream(Heightmap.Type.values()).collect(Collectors.toUnmodifiableSet());

    /**
     * Regenerate the Heightmaps of the specified chunk
     * @param chunk the chunk to edit
     */
    public static void resetHeightMaps(Chunk chunk) {

        Heightmap.populateHeightmaps(chunk , ALL_HEIGHTMAPS);

    }

    /**
     * Remove all entites from the provided chunk
     * skip entities above y0 if the chunk was datafixed
     * @param chunk the chunk to clear
     * @param world the world the chunk belongs to
     */
    public static void clearEntities(ProtoChunk chunk, ServerWorld world) {
        ChunkStatus old_status = ((RetrogenHolder)chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isAtLeast(ChunkStatus.INITIALIZE_LIGHT);

        if (had_retrogen) {
            //erase only entities below y0
            Iterator<NbtCompound> entityIterator = chunk.getEntities().iterator();
            while (entityIterator.hasNext()) {
                NbtCompound entity_nbt = entityIterator.next();
                NbtList entity_pos_list = entity_nbt.getList("Pos", NbtElement.DOUBLE_TYPE);
                BlockPos entity_pos = new BlockPos(MathHelper.floor(entity_pos_list.getDouble(0)), MathHelper.floor(entity_pos_list.getDouble(1)), MathHelper.floor(entity_pos_list.getDouble(2)));
                if (!BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(entity_pos))
                    entityIterator.remove();
            }
        }else{
            //erase all
            chunk.getEntities().clear();
        }

    }

    /**
     * Place the graced blocks back into the chunk
     * Limit to blocks below y0 if this chunk is datafixed
     * @param chunk the chunk to edit
     * @param world the world the chunk belongs to
     */
    public static void restoreBlocks(Chunk chunk, ServerWorld world) {
        ChunkStatus old_status = ((RetrogenHolder)chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isAtLeast(ChunkStatus.INITIALIZE_LIGHT);

        Map<BlockPos, BlockState> gracedBlocks = ((GraceHolder)chunk).protoSky$getGracedBlocks();

        if (Debug.chunkOriginBlock != null){
            gracedBlocks.put(chunk.getPos().getStartPos(), Debug.chunkOriginBlock.getDefaultState());
        }

        gracedBlocks.forEach((blockPos, blockState) -> {
            if (!had_retrogen || !BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(blockPos)){
                chunk.setBlockState(blockPos,blockState,false);
            }
        });
    }

    /**
     * Place the graced entities back into the chunk
     * Limit to entities below y0 if this chunk is datafixed
     * @param chunk the chunk to edit
     * @param world the world the chunk belongs to
     */
    public static void restoreEntities(ProtoChunk chunk, ServerWorld world) {
        ChunkStatus old_status = ((RetrogenHolder) chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isAtLeast(ChunkStatus.INITIALIZE_LIGHT);

        Set<NbtCompound> gracedEntities = ((GraceHolder) chunk).protoSky$getGracedEntities();

        Stream<NbtCompound> entity_stream = gracedEntities.stream();

        if (had_retrogen){
            //filter out entities above y0
            entity_stream = entity_stream.filter(entity_nbt -> {
                NbtList entity_pos_list = entity_nbt.getList("Pos", NbtElement.DOUBLE_TYPE);
                BlockPos entity_pos = new BlockPos(MathHelper.floor(entity_pos_list.getDouble(0)), MathHelper.floor(entity_pos_list.getDouble(1)), MathHelper.floor(entity_pos_list.getDouble(2)));
                return !BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(entity_pos);
            });
        }

        EntityType.streamFromNbt(entity_stream.toList(), world).forEach(world::spawnEntity);

    }
}
