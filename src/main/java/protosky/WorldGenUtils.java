package protosky;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
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
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import protosky.mixins.ProtoChunkAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static protosky.ProtoSkySettings.LOGGER;

public class WorldGenUtils
{
    public static void deleteBlocks(Chunk chunk, ServerWorld world) {
        //This loops through all sections (16x16x16) sections of a chunk and copies over the biome information, but not the blocks.
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection chunkSection = sections[i];

            //This section finds all the budding_amethyst notes where they are
            List<Integer> buddingsX = new ArrayList<>();
            List<Integer> buddingsY = new ArrayList<>();
            List<Integer> buddingsZ = new ArrayList<>();

            for(int x = 0; x < 16; x++) for(int z = 0; z < 16; z++) for(int y = 0; y < 16; y++) {
                if (chunkSection.getBlockState(x,y,z).toString().contains("budding_amethyst")) {
                    buddingsX.add(x);
                    buddingsY.add(y);
                    buddingsZ.add(z);
                }
            }

            PalettedContainer<BlockState> blockStateContainer = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);

            //This section copies over all those budding_amethyst to keep them.
            int counter = 0;
            for(int x : buddingsX) {
                int y = buddingsY.get(counter);
                int z = buddingsZ.get(counter);
                blockStateContainer.set(x, y, z, Registries.BLOCK.get(Identifier.tryParse("minecraft:budding_amethyst")).getDefaultState());

                LOGGER.info("Amathyst at " + (x + chunk.getPos().getStartX()) + " " + (z + chunk.getPos().getStartZ()));
                counter++;
            }
            //LOGGER.info("Counter is " + counter);

            ReadableContainer<RegistryEntry<Biome>> biomeContainer = chunkSection.getBiomeContainer();
            sections[i] = new ChunkSection(blockStateContainer, biomeContainer);
        }

        //This removes all the block entities
        for (BlockPos bePos : chunk.getBlockEntityPositions()) {
            chunk.removeBlockEntity(bePos);
        }

        //This should clear all the light sources, but with the 1.20 lighting update it's not needed.
        /*ProtoChunk protoChunk = (ProtoChunk) chunk;
        ProtoChunkAccessor protoChunkAccessor = (ProtoChunkAccessor) protoChunk;
        LightingProvider lightingProvider = protoChunkAccessor.getLightingProvider();
        LightingProvider lightingProvider = world.getLightingProvider();
        lightingProvider.doLightUpdates();
        chunk.getLightSources().clear();*/
    }

    public static void genHeightMaps(Chunk chunk) {
        // defined in Heightmap class constructor
        int elementBits = MathHelper.ceilLog2(chunk.getHeight() + 1);
        long[] emptyHeightmap = new PackedIntegerArray(elementBits, 256).getData();
        for (Map.Entry<Heightmap.Type, Heightmap> heightmapEntry : chunk.getHeightmaps())
        {
            heightmapEntry.getValue().setTo(chunk, heightmapEntry.getKey(), emptyHeightmap);
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
