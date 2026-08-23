package protosky.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import protosky.Debug;
import protosky.interfaces.GraceHolder;
import protosky.interfaces.RetrogenHolder;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class WorldGenUtils {

    /**
     * Empty all chunkSection of the provided chunk.
     * Skip sections above y0 if this was a datafixed Chunk
     *
     * @param chunk the chunk to clear
     * @param world the world the chunk belongs to
     */
    public static void deleteBlocks(ChunkAccess chunk, ServerLevel world) {
        //fixes for RetroGen
        ChunkStatus old_status = ((RetrogenHolder) chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);
        //This loops through all sections (16x16x16) sections of a chunk and copies over the biome information, but not the blocks.
        LevelChunkSection[] sections = chunk.getSections();
        Map<BlockPos, BlockState> gracedBlocks = ((GraceHolder) chunk).protoSky$getGracedBlocks();
        for (int i = 0; i < sections.length; i++) {
            //avoid deleting blocks from pre 1.18 chunks
            if (had_retrogen && BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR.isOutsideBuildHeight(chunk.getSectionYFromSectionIndex(i)))
                continue;

            //clear the chunk
            LevelChunkSection chunkSection = sections[i];
            PalettedContainer<BlockState> blockStateContainer = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);

            PalettedContainerRO<Holder<Biome>> biomeContainer = chunkSection.getBiomes();
            sections[i] = new LevelChunkSection(blockStateContainer, biomeContainer);
        }

        //This removes all the block entities
        for (BlockPos bePos : chunk.getBlockEntitiesPos()) {
            //avoid deleting blocks from pre 1.18 chunks
            if (had_retrogen && BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR.isOutsideBuildHeight(bePos.getY()))
                continue;
            //avoid deleting blockEntities that have been graced
            if (!gracedBlocks.containsKey(bePos))
                chunk.removeBlockEntity(bePos);
        }

    }

    /**
     * Regenerate the Heightmaps of the specified chunk
     *
     * @param chunk the chunk to edit
     */
    public static void resetHeightMaps(ChunkAccess chunk) {
        //fixes for RetroGen
        ChunkStatus old_status = ((RetrogenHolder) chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);

        //keep old heightmaps if we had retrogen
        if (had_retrogen)
            return;

        for (Heightmap.Types type : ChunkStatus.POST_FEATURES) {
            Heightmap map = chunk.getOrCreateHeightmapUnprimed(type);
            chunk.setHeightmap(type, new long[map.getRawData().length]);
        }
    }

    /**
     * Remove all entites from the provided chunk
     * skip entities above y0 if the chunk was datafixed
     *
     * @param chunk the chunk to clear
     * @param world the world the chunk belongs to
     */
    public static void clearEntities(ProtoChunk chunk, ServerLevel world) {
        ChunkStatus old_status = ((RetrogenHolder) chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);

        if (had_retrogen) {
            //erase only entities below y0
            Iterator<CompoundTag> entityIterator = chunk.getEntities().iterator();
            while (entityIterator.hasNext()) {
                CompoundTag entity_nbt = entityIterator.next();
                ListTag entity_pos_list = entity_nbt.getList("Pos", Tag.TAG_DOUBLE);
                BlockPos entity_pos = new BlockPos(Mth.floor(entity_pos_list.getDouble(0)), Mth.floor(entity_pos_list.getDouble(1)), Mth.floor(entity_pos_list.getDouble(2)));
                if (!BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR.isOutsideBuildHeight(entity_pos))
                    entityIterator.remove();
            }
        } else {
            //erase all
            chunk.getEntities().clear();
        }

    }

    /**
     * Place the graced blocks back into the chunk
     * Limit to blocks below y0 if this chunk is datafixed
     *
     * @param chunk the chunk to edit
     * @param world the world the chunk belongs to
     */
    public static void restoreBlocks(ChunkAccess chunk, ServerLevel world) {
        ChunkStatus old_status = ((RetrogenHolder) chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);

        Map<BlockPos, BlockState> gracedBlocks = ((GraceHolder) chunk).protoSky$getGracedBlocks();

        if (Debug.chunkOriginBlock != null) {
            gracedBlocks.put(chunk.getPos().getWorldPosition(), Debug.chunkOriginBlock.defaultBlockState());
        }

        gracedBlocks.forEach((blockPos, blockState) -> {
            if (!had_retrogen || !BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR.isOutsideBuildHeight(blockPos)) {
                chunk.setBlockState(blockPos, blockState, false);
            }
        });
    }

    /**
     * Place the graced entities back into the chunk
     * Limit to entities below y0 if this chunk is datafixed
     *
     * @param chunk the chunk to edit
     * @param world the world the chunk belongs to
     */
    public static void restoreEntities(ProtoChunk chunk, ServerLevel world) {
        ChunkStatus old_status = ((RetrogenHolder) chunk).protoSky$getPreviousStatus();
        boolean had_retrogen = old_status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);

        Set<CompoundTag> gracedEntities = ((GraceHolder) chunk).protoSky$getGracedEntities();

        Stream<CompoundTag> entity_stream = gracedEntities.stream();

        if (had_retrogen) {
            //filter out entities above y0
            entity_stream = entity_stream.filter(entity_nbt -> {
                ListTag entity_pos_list = entity_nbt.getList("Pos", Tag.TAG_DOUBLE);
                BlockPos entity_pos = new BlockPos(Mth.floor(entity_pos_list.getDouble(0)), Mth.floor(entity_pos_list.getDouble(1)), Mth.floor(entity_pos_list.getDouble(2)));
                return !BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR.isOutsideBuildHeight(entity_pos);
            });
        }

        EntityType.loadEntitiesRecursive(entity_stream.toList(), world).forEach(world::addFreshEntity);

    }
}
