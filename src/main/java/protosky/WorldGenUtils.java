package protosky;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import protosky.interfaces.RetrogenHolder;
import protosky.mixins.ProtoChunkAccessor;

import java.util.Map;
import java.util.Optional;

import static protosky.ProtoSkySettings.LOGGER;

public class WorldGenUtils
{
    public static void deleteBlocks(Chunk chunk, ServerWorld world) {
        //fixes for RetroGen
        boolean had_retrogen = ((RetrogenHolder)chunk).wasBelowZeroRetrogen();
        //This loops through all sections (16x16x16) sections of a chunk and copies over the biome information, but not the blocks.
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            if (had_retrogen && BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(chunk.sectionIndexToCoord(i)))
                continue;
            ChunkSection chunkSection = sections[i];
            PalettedContainer<BlockState> blockStateContainer = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);
            ReadableContainer<RegistryEntry<Biome>> biomeContainer = chunkSection.getBiomeContainer();
            sections[i] = new ChunkSection(blockStateContainer, biomeContainer);
        }

        //This removes all the block entities
        for (BlockPos bePos : chunk.getBlockEntityPositions()) {
            if (had_retrogen && BelowZeroRetrogen.BELOW_ZERO_VIEW.isOutOfHeightLimit(bePos.getY()))
                continue;
            chunk.removeBlockEntity(bePos);
        }

        //This should clear all the light sources
        /*ProtoChunk protoChunk = (ProtoChunk) chunk;
        ProtoChunkAccessor protoChunkAccessor = (ProtoChunkAccessor) protoChunk;
        LightingProvider lightingProvider = protoChunkAccessor.getLightingProvider();
        LightingProvider lightingProvider = world.getLightingProvider();
        lightingProvider.doLightUpdates();
        chunk.getLightSources().clear();*/
    }

    public static void genHeightMaps(Chunk chunk) {
        boolean had_retrogen = ((RetrogenHolder)chunk).wasBelowZeroRetrogen();
        // defined in Heightmap class constructor
        int elementBits = MathHelper.ceilLog2(chunk.getHeight() + 1);
        long[] emptyHeightmap = new PackedIntegerArray(elementBits, 256).getData();
        for (Map.Entry<Heightmap.Type, Heightmap> heightmapEntry : chunk.getHeightmaps())
        {
            //fix heightmap if this was an old Chunk
            if (had_retrogen){
                Heightmap heightmap = heightmapEntry.getValue();
                for (int x=0; x<16;x++){
                    for (int z=0; z<16;z++){
                        int val = heightmap.get(x,z);
                        heightmap.set(x,z,(val>=BelowZeroRetrogen.BELOW_ZERO_VIEW.getTopY())?val:0);
                    }
                }
            }else {
                heightmapEntry.getValue().setTo(chunk, heightmapEntry.getKey(), emptyHeightmap);
            }
        }
    }


    public static void clearEntities(ProtoChunk chunk, ServerWorld world) {
        // erase entities
        if (!(world.getRegistryKey() == World.END)) {
            chunk.getEntities().clear();
        } else {
            chunk.getEntities().removeIf(tag -> {
                String id = tag.getString("id");
                return !id.equals("minecraft:end_crystal") && !id.equals("minecraft:shulker") && !id.equals("minecraft:item_frame");
            });
        }
    }

    public static void genSpawnPlatform(Chunk chunk, ServerWorld world) {
        StructureTemplateManager man = world.getStructureTemplateManager();
        StructureTemplate s = null;

        // Get structure for this dimension
        if (world.getRegistryKey() == World.OVERWORLD) {
            Optional<StructureTemplate> op = man.getTemplate(new Identifier("protosky:spawn_overworld"));
            if (op.isPresent()) {
                s = op.get();
            }
        } else if (world.getRegistryKey() == World.NETHER) {
            Optional<StructureTemplate> op = man.getTemplate(new Identifier("protosky:spawn_nether"));
            if (op.isPresent()) {
                s = op.get();
            }
        }
        if (s == null) return;

        ChunkPos chunkPos = chunk.getPos();
        BlockPos blockPos = new BlockPos(chunkPos.x * 16, 64, chunkPos.z * 16);

        StructurePlacementData structurePlacementData = new StructurePlacementData().setUpdateNeighbors(true);

        int flags = 0;
        s.place(world, blockPos, blockPos, structurePlacementData, new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()), flags);
        world.setSpawnPos(blockPos.add(s.getSize().getX() / 2, s.getSize().getY() + 1, s.getSize().getZ() / 2), 0);
    }
}
