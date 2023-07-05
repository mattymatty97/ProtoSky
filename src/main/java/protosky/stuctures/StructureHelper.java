package protosky.stuctures;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.*;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.BlockRotStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import protosky.mixins.PlacedFeatureAccessor;
import protosky.mixins.StructureHelperInvokers.*;
import protosky.mixins.StructurePieceAccessor;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static protosky.ProtoSkySettings.LOGGER;

public class StructureHelper {
    @FunctionalInterface
    public interface NineArgumentFunction<ReturnType, Arg1Type, Arg2Type, Arg3Type, Arg4Type, Arg5Type, Arg6Type, Arg7Type, Arg8Type, Arg9Type> {
        ReturnType apply(Arg1Type arg1, Arg2Type arg2, Arg3Type arg3, Arg4Type arg4, Arg5Type arg5, Arg6Type arg6, Arg7Type arg7, Arg8Type arg8, Arg9Type arg9);
    }
    public static boolean ran = false;
    private static final List<Pair<Structure, NineArgumentFunction<Boolean, StructurePiece, StructureWorldAccess, StructureAccessor, ChunkGenerator, Random, BlockBox, ChunkPos, BlockPos, Chunk>>> structuresAfterDelete = new ArrayList<>();
    private static final List<Pair<Structure, NineArgumentFunction<Boolean, StructurePiece, StructureWorldAccess, StructureAccessor, ChunkGenerator, Random, BlockBox, ChunkPos, BlockPos, Chunk>>> structuresBeforeDelete = new ArrayList<>();


    //These functions register all the structure handlers that get called in handleStructureStart(). The format of a handler
    // is a pair of a Registry<Structure> that has the structure to run on and a function that will get called on when
    // that structure is found. Its arguments are the ones that StructurePiece.generate() would get plus the current chunk.
    // If it returns true then StructurePiece.generate() is called. If false it is not ran. This is a function and not static
    // because in singleplayer the Registry<Structure> changes every time you change worlds causing it to fail if two worlds
    // are loading in one session. It gets called by TAGS_LOADED event in fixWorldLoads.java.
    private static void registerStructuresAfterDelete(WorldAccess world) {
        Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        //All of these are mostly copied verbatim from their Generator class (net.minecraft.structure.<structure name>Generator)
        // with minor changes to use Accessors and Invokers. Some are also refactored for readability.
        structuresAfterDelete.clear();
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("stronghold")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2, chunk) -> {
                    if (structurePiece instanceof StrongholdGenerator.PortalRoom) {

                        BlockState northFrame = Blocks.END_PORTAL_FRAME.getDefaultState().with(EndPortalFrameBlock.FACING, Direction.NORTH);
                        BlockState southFrame = Blocks.END_PORTAL_FRAME.getDefaultState().with(EndPortalFrameBlock.FACING, Direction.SOUTH);
                        BlockState eastFrame = Blocks.END_PORTAL_FRAME.getDefaultState().with(EndPortalFrameBlock.FACING, Direction.EAST);
                        BlockState westFrame = Blocks.END_PORTAL_FRAME.getDefaultState().with(EndPortalFrameBlock.FACING, Direction.WEST);

                        boolean filled = true;
                        boolean[] frameHasEye = new boolean[12];

                        for(int l = 0; l < frameHasEye.length; ++l) {
                            //FIX: This random needs to equal vanilla
                            frameHasEye[l] = random.nextFloat() > 0.9F;
                            filled &= frameHasEye[l];
                        }

                        setBlockInStructure(structurePiece, chunk, northFrame.with(EndPortalFrameBlock.EYE, frameHasEye[0]), 4, 3, 8);
                        setBlockInStructure(structurePiece, chunk, northFrame.with(EndPortalFrameBlock.EYE, frameHasEye[1]), 5, 3, 8);
                        setBlockInStructure(structurePiece, chunk, northFrame.with(EndPortalFrameBlock.EYE, frameHasEye[2]), 6, 3, 8);
                        setBlockInStructure(structurePiece, chunk, southFrame.with(EndPortalFrameBlock.EYE, frameHasEye[3]), 4, 3, 12);
                        setBlockInStructure(structurePiece, chunk, southFrame.with(EndPortalFrameBlock.EYE, frameHasEye[4]), 5, 3, 12);
                        setBlockInStructure(structurePiece, chunk, southFrame.with(EndPortalFrameBlock.EYE, frameHasEye[5]), 6, 3, 12);
                        setBlockInStructure(structurePiece, chunk, eastFrame.with(EndPortalFrameBlock.EYE, frameHasEye[6]), 3, 3, 9);
                        setBlockInStructure(structurePiece, chunk, eastFrame.with(EndPortalFrameBlock.EYE, frameHasEye[7]), 3, 3, 10);
                        setBlockInStructure(structurePiece, chunk, eastFrame.with(EndPortalFrameBlock.EYE, frameHasEye[8]), 3, 3, 11);
                        setBlockInStructure(structurePiece, chunk, westFrame.with(EndPortalFrameBlock.EYE, frameHasEye[9]), 7, 3, 9);
                        setBlockInStructure(structurePiece, chunk, westFrame.with(EndPortalFrameBlock.EYE, frameHasEye[10]), 7, 3, 10);
                        setBlockInStructure(structurePiece, chunk, westFrame.with(EndPortalFrameBlock.EYE, frameHasEye[11]), 7, 3, 11);
                        if (filled) {
                            BlockState endPortal = Blocks.END_PORTAL.getDefaultState();
                            setBlockInStructure(structurePiece, chunk, endPortal, 4, 3, 9);
                            setBlockInStructure(structurePiece, chunk, endPortal, 5, 3, 9);
                            setBlockInStructure(structurePiece, chunk, endPortal, 6, 3, 9);
                            setBlockInStructure(structurePiece, chunk, endPortal, 4, 3, 10);
                            setBlockInStructure(structurePiece, chunk, endPortal, 5, 3, 10);
                            setBlockInStructure(structurePiece, chunk, endPortal, 6, 3, 10);
                            setBlockInStructure(structurePiece, chunk, endPortal, 4, 3, 11);
                            setBlockInStructure(structurePiece, chunk, endPortal, 5, 3, 11);
                            setBlockInStructure(structurePiece, chunk, endPortal, 6, 3, 11);
                        }

                    }
                    return false;
                })
        );
        //Structure Move handlers
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("swamp_hut")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2, chunk) -> {
                    if(structurePiece instanceof ShiftableStructurePiece) {
                        SwampHutGenerator This = ((SwampHutGenerator)(Object)structurePiece);
                        ShiftableStructurePieceInvoker shiftableStructurePieceInvoker = ((ShiftableStructurePieceInvoker) This);
                        shiftableStructurePieceInvoker.invokeAdjustToAverageHeight(worldAccess, chunkBox, 0);
                    }
                    return false;
                })
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("jungle_pyramid")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2, chunk) -> {
                    if(structurePiece instanceof ShiftableStructurePiece) {
                        JungleTempleGenerator This = ((JungleTempleGenerator)(Object)structurePiece);
                        ShiftableStructurePieceInvoker shiftableStructurePieceInvoker = ((ShiftableStructurePieceInvoker) This);
                        shiftableStructurePieceInvoker.invokeAdjustToAverageHeight(worldAccess, chunkBox, 0);
                    }
                    return false;
                })
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("desert_pyramid")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2, chunk) -> {
                    if(structurePiece instanceof ShiftableStructurePiece) {
                        DesertTempleGenerator This = ((DesertTempleGenerator)(Object)structurePiece);
                        ShiftableStructurePieceInvoker shiftableStructurePieceInvoker = ((ShiftableStructurePieceInvoker) This);
                        shiftableStructurePieceInvoker.invokeAdjustToMinHeight(worldAccess, -random.nextInt(3));
                    }
                    return false;
                })
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("igloo")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot, chunk) -> {
                    SimpleStructurePieceInvoker simplePieceInvoker = ((SimpleStructurePieceInvoker) structurePiece);
                    IglooGeneratorPieceInvoker iglooGeneratorPieceInvoker = ((IglooGeneratorPieceInvoker) structurePiece);
                    StructurePieceInvoker pieceInvoker = (StructurePieceInvoker) structurePiece;


                    final Identifier TOP_TEMPLATE = new Identifier("igloo/top");
                    final Identifier MIDDLE_TEMPLATE = new Identifier("igloo/middle");
                    final Identifier BOTTOM_TEMPLATE = new Identifier("igloo/bottom");
                    final Map<Identifier, BlockPos> OFFSETS_FROM_TOP = ImmutableMap.of(
                            TOP_TEMPLATE, BlockPos.ORIGIN, MIDDLE_TEMPLATE, new BlockPos(2, -3, 4), BOTTOM_TEMPLATE, new BlockPos(0, -3, -2)
                    );

                    Identifier identifier = new Identifier(simplePieceInvoker.getTemplateIdString());
                    StructurePlacementData structurePlacementData = iglooGeneratorPieceInvoker.invokeCreatePlacementData(simplePieceInvoker.getPlacementData().getRotation(), identifier);

                    BlockPos blockPos = OFFSETS_FROM_TOP.get(identifier);
                    BlockPos blockPos2 = simplePieceInvoker.getPos().add(StructureTemplate.transform(structurePlacementData, new BlockPos(3 - blockPos.getX(), 0, -blockPos.getZ())));

                    int i = worldAccess.getTopY(Heightmap.Type.WORLD_SURFACE_WG, blockPos2.getX(), blockPos2.getZ());
                    BlockPos posCache = simplePieceInvoker.getPos();

                    simplePieceInvoker.setPos(simplePieceInvoker.getPos().add(0, i - 90 - 1, 0));

                    simplePieceInvoker.getPlacementData().setBoundingBox(chunkBox);
                    pieceInvoker.setBoundingBox(simplePieceInvoker.getTemplate().calculateBoundingBox(simplePieceInvoker.getPlacementData(), simplePieceInvoker.getPos()));

                    if (identifier.equals(TOP_TEMPLATE)) {
                        BlockPos blockPos4 = simplePieceInvoker.getPos().add(StructureTemplate.transform(structurePlacementData, new BlockPos(3, 0, 5)));
                    }

                    simplePieceInvoker.setPos(posCache);

                    //return true;
                    return false;
                })
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("shipwreck")),
                //This is looks different than the original, but all that happened to it is it was restructured and named.
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot, chunk) -> {
                    SimpleStructurePieceInvoker simplePieceInvoker = ((SimpleStructurePieceInvoker) structurePiece);
                    ShipwreckGeneratorPieceInvoker shipwreckGeneratorPieceInvoker  = ((ShipwreckGeneratorPieceInvoker) structurePiece);
                    StructurePieceInvoker pieceInvoker = (StructurePieceInvoker) structurePiece;

                    Heightmap.Type heightmap = shipwreckGeneratorPieceInvoker.getGrounded() ? Heightmap.Type.WORLD_SURFACE_WG : Heightmap.Type.OCEAN_FLOOR_WG;

                    Vec3i size = simplePieceInvoker.getTemplate().getSize();
                    int area2D = size.getX() * size.getZ();
                    int YtoMoveTo = 0;

                    //Beached Shipwrecks
                    if(shipwreckGeneratorPieceInvoker.getGrounded()) {
                        //This moves it down to the minimum height of the world heightmap.
                        int minimumHeight = worldAccess.getTopY();
                        //Run through the 2D locations of the structure and get the average of their heightmaps
                        BlockPos otherBottomCorner = simplePieceInvoker.getPos().add(size.getX() - 1, 0, size.getZ() - 1);
                        //Run through the 2D locations of the structure and get the average of their heightmaps
                        for (BlockPos bottomLayerBlock : BlockPos.iterate(simplePieceInvoker.getPos(), otherBottomCorner)) {
                            minimumHeight = Math.min(minimumHeight, worldAccess.getTopY(heightmap, bottomLayerBlock.getX(), bottomLayerBlock.getZ()));
                        }
                        //FIX: This random needs to equal vanilla
                        YtoMoveTo = minimumHeight - size.getY() / 2 - random.nextInt(3);

                        //Normal shipwrecks
                    } else {
                        //Basically this just averages all the heights of ocean floor heightmap and puts it there.
                        int accumulatedHeight = 0;
                        //Run through the 2D locations of the structure and get the average of their heightmaps
                        BlockPos otherBottomCorner = simplePieceInvoker.getPos().add(size.getX() - 1, 0, size.getZ() - 1);
                        //Run through the 2D locations of the structure and get the average of their heightmaps
                        for (BlockPos bottomLayerBlock : BlockPos.iterate(simplePieceInvoker.getPos(), otherBottomCorner)) {
                            accumulatedHeight += worldAccess.getTopY(heightmap, bottomLayerBlock.getX(), bottomLayerBlock.getZ());
                        }
                        YtoMoveTo = accumulatedHeight / area2D;
                    }

                    simplePieceInvoker.setPos(new BlockPos(simplePieceInvoker.getPos().getX(), YtoMoveTo, simplePieceInvoker.getPos().getZ()));
                    //super.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot);
                    simplePieceInvoker.getPlacementData().setBoundingBox(chunkBox);
                    pieceInvoker.setBoundingBox(simplePieceInvoker.getTemplate().calculateBoundingBox(simplePieceInvoker.getPlacementData(), simplePieceInvoker.getPos()));

                    return false;
                })
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("shipwreck_beached")),
                structuresAfterDelete.get(structuresAfterDelete.size() - 1).getRight())
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("ocean_ruin_cold")),
                //This is looks different than the original, but all that happened to it is it was restructured and named.
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot, chunk) -> {
                    SimpleStructurePieceInvoker simplePieceInvoker = ((SimpleStructurePieceInvoker) structurePiece);
                    OceanRuinGeneratorPieceInvoker oceanRuinGeneratorPieceInvoker = ((OceanRuinGeneratorPieceInvoker) structurePiece);
                    StructurePieceInvoker pieceInvoker = (StructurePieceInvoker) structurePiece;

                    simplePieceInvoker.getPlacementData()
                            .clearProcessors()
                            .addProcessor(new BlockRotStructureProcessor(oceanRuinGeneratorPieceInvoker.getIntegrity()))
                            .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);

                    int i = worldAccess.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, simplePieceInvoker.getPos().getX(), simplePieceInvoker.getPos().getZ());
                    simplePieceInvoker.setPos(new BlockPos(simplePieceInvoker.getPos().getX(), i, simplePieceInvoker.getPos().getZ()));

                    BlockPos otherCorner = StructureTemplate.transformAround(
                            new BlockPos(simplePieceInvoker.getTemplate().getSize().getX() - 1, 0, simplePieceInvoker.getTemplate().getSize().getZ() - 1),
                            BlockMirror.NONE,
                            simplePieceInvoker.getPlacementData().getRotation(),
                            BlockPos.ORIGIN
                    ).add(simplePieceInvoker.getPos());

                    BlockPos structurePos = simplePieceInvoker.getPos();

                    //This is method_14829 inlined, but it has been changed to use the heightmap instead of using the physical blocks that get deleted.
                    int yToMoveTo = structurePos.getY();
                    int initialY = structurePos.getY() - 1;

                    int lowestSpot = 512;
                    int bottomOfWorld = 0;

                    for(BlockPos blockPos : BlockPos.iterate(structurePos, otherCorner)) {
                        int y = worldAccess.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, blockPos.getX(), blockPos.getZ());
                        lowestSpot = Math.min(lowestSpot, y);

                        if (y < initialY - 2) {
                            bottomOfWorld++;
                        }
                    }

                    int p = Math.abs(structurePos.getX() - otherCorner.getX());
                    if (initialY - lowestSpot > 2 && bottomOfWorld > p - 2) {
                        yToMoveTo = lowestSpot + 1;
                    }

                    //int yToMoveTo = oceanRuinYCalculator(simplePieceInvoker.getPos(), worldAccess, otherCorner);

                    simplePieceInvoker.setPos(new BlockPos(simplePieceInvoker.getPos().getX(), yToMoveTo, simplePieceInvoker.getPos().getZ()));
                    simplePieceInvoker.getPlacementData().setBoundingBox(chunkBox);
                    pieceInvoker.setBoundingBox(simplePieceInvoker.getTemplate().calculateBoundingBox(simplePieceInvoker.getPlacementData(), simplePieceInvoker.getPos()));
                    return false;
                })
        );
        structuresAfterDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("ocean_ruin_warm")),
                structuresAfterDelete.get(structuresAfterDelete.size() - 1).getRight())
        );

    }
    private static void registerStructuresBeforeDelete(WorldAccess world) {
        Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        //These are ran before all the blocks are deleted for the structures that need that.
        structuresBeforeDelete.clear();
        structuresBeforeDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("buried_treasure")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot, chunk) -> {
                    StructurePieceInvoker pieceInvoker = (StructurePieceInvoker) structurePiece;

                    int i = worldAccess.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, pieceInvoker.getBoundingBox().getMinX(), pieceInvoker.getBoundingBox().getMinZ());
                    BlockPos.Mutable mutable = new BlockPos.Mutable(pieceInvoker.getBoundingBox().getMinX(), i, pieceInvoker.getBoundingBox().getMinZ());

                    while(mutable.getY() > worldAccess.getBottomY()) {
                        BlockState blockState2 = worldAccess.getBlockState(mutable.down());
                        if (blockState2 == Blocks.SANDSTONE.getDefaultState()
                                || blockState2 == Blocks.STONE.getDefaultState()
                                || blockState2 == Blocks.ANDESITE.getDefaultState()
                                || blockState2 == Blocks.GRANITE.getDefaultState()
                                || blockState2 == Blocks.DIORITE.getDefaultState()) {
                            break;
                        }

                        mutable.move(0, -1, 0);
                    }
                    pieceInvoker.setBoundingBox(new BlockBox(mutable));
                    return false;
                })
        );
        structuresBeforeDelete.add(new MutablePair<>(
                structureRegistry.get(Identifier.tryParse("end_city")),
                (structurePiece, worldAccess, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2, chunk) -> {
                    LOGGER.info("Ran end city");
                    return true;
                })
        );

    }

    //This gets called by an entrypoint to refresh all the handlers as the registry key changes every time you open and close the world
    private static synchronized void fixRaceCondition(WorldAccess world) {
        if (!ran) {
            registerStructuresBeforeDelete(world);
            registerStructuresAfterDelete(world);

            ran = true;
        }
    }


    //This stuff is all used to detect structure moves when generating structures. It prints out the location of the bounding
    // boxes of each of the pieces before and after the handler has been called on them. It also prints out the movement
    // during generation. This basically does what the findStructureMoves mod does except for all structures not just the
    // one it's looking for at the moment. The detectStructureMoved mod doesn't work here because it hooks into StructureStart.place()
    // to detect the movement of a structure but the ProtoSky mod uses its own called handleStructureStarts(). It also doesn't
    // work because not all structures are generated by this mod and it doesn't know which ones are, so even if it hooked
    // into the correct function it would wait forever for to get the generation of a structure that would never generate.
    private static BlockBox cloneBlockBox(BlockBox blockBox) {
        return new BlockBox(blockBox.getMinX(), blockBox.getMinY(), blockBox.getMinZ(), blockBox.getMaxX(), blockBox.getMaxY(), blockBox.getMaxZ());
    }
    private static Vec3i calculateBlockBoxDelta(BlockBox old, BlockBox New) {
        return new Vec3i(old.getMinX() - New.getMinX(), old.getMinY() - New.getMinY(), old.getMinZ() - New.getMinZ());
    }

    private static boolean blockBoxMoved(BlockBox old, BlockBox New) {
        return old.getMinX() != New.getMinX() || old.getMinY() != New.getMinY() || old.getMinZ() != New.getMinZ() || old.getMaxX() != New.getMaxX() || old.getMaxY() != New.getMaxY() || old.getMaxZ() != New.getMaxZ();
    }

    //Use this to enable bounding movement printing
    private static final boolean enableMovementDetection = false;


    //Back to normal but with checks to see if we have bounding box movement checking on.
    //This function takes a structure start (those basically say put a structure, and if it has parts its parts, here) and a handler
    // and loops through all the pieces in the structure start running the provided handler on each one.
    private static void handleStructureStart(StructureStart structureStart, StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos,
                                             NineArgumentFunction<Boolean, StructurePiece, StructureWorldAccess, StructureAccessor, ChunkGenerator, Random, BlockBox, ChunkPos, BlockPos, Chunk> handler, Chunk chunk) {
        List<StructurePiece> structurePieces = structureStart.getChildren();
        if (!structurePieces.isEmpty()) {
            BlockBox firstPieceBoundBox = structurePieces.get(0).getBoundingBox();
            BlockPos centerBlock = firstPieceBoundBox.getCenter();
            BlockPos bottomCenterBlockBox = new BlockPos(centerBlock.getX(), firstPieceBoundBox.getMinY(), centerBlock.getZ());

            //This is the check.
            if(enableMovementDetection) {
                ArrayList<net.minecraft.util.Pair<BlockBox, BlockBox>> partBoxes = new ArrayList<>();
                BlockBox partBoundingBoxOld;

                for(StructurePiece structurePiece : structurePieces) {
                    if (structurePiece.getBoundingBox().intersects(chunkBox)) {
                        partBoundingBoxOld = cloneBlockBox(structurePiece.getBoundingBox());

                        if(handler.apply(structurePiece, world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, bottomCenterBlockBox, chunk)) {
                            structurePiece.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, bottomCenterBlockBox);
                        }

                        partBoxes.add(new net.minecraft.util.Pair<>(partBoundingBoxOld, structurePiece.getBoundingBox()));
                    }
                }

                if(partBoxes.size() > 0 && partBoxes.get(0) != null) {
                    //Print out the movement
                    StringBuilder stringBuilder = new StringBuilder();
                    Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

                    stringBuilder.append(structureRegistry.getId(structureStart.getStructure()).toString());
                    stringBuilder.append(" at ");
                    stringBuilder.append(partBoxes.get(0).getLeft().getMinX() + ", " + partBoxes.get(0).getLeft().getMinZ());

                    if(partBoxes.size() == 1) {
                        net.minecraft.util.Pair<BlockBox, BlockBox> boxes = partBoxes.get(0);
                        if(!(boxes.getLeft() == null || boxes.getRight() == null)) {
                            stringBuilder.append(blockBoxMoved(partBoxes.get(0).getLeft(), partBoxes.get(0).getRight()) ? " moved by " + calculateBlockBoxDelta(boxes.getLeft(), boxes.getRight()) + " from " + boxes.getLeft().getCenter() + " to " + boxes.getRight().getCenter() : " didn't move");
                        }
                    } else {
                        int iii = 0;

                        stringBuilder.append("\n");
                        for(net.minecraft.util.Pair<BlockBox, BlockBox> boxes : partBoxes) {
                            iii++;
                            stringBuilder
                                    .append("  Part ").append(iii)
                                    .append(blockBoxMoved(boxes.getLeft(), boxes.getRight()) ?
                                            " moved by " + calculateBlockBoxDelta(boxes.getLeft(), boxes.getRight()) + " from " + boxes.getLeft().getCenter() + " to " + boxes.getRight().getCenter() :
                                            " didn't move"
                                    ).append("\n");
                        }
                        stringBuilder.deleteCharAt(stringBuilder.length()-1);
                    }
                    LOGGER.info(stringBuilder.toString());
                }

            } else {
                for(StructurePiece structurePiece : structurePieces) {
                    if (structurePiece.getBoundingBox().intersects(chunkBox)) {
                        if(handler.apply(structurePiece, world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, bottomCenterBlockBox, chunk)) {
                            structurePiece.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, bottomCenterBlockBox);
                        }
                    }
                }
            }

            structureStart.getStructure().postPlace(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, ((StructureStartInvoker) (Object) structureStart).getRealChildren());
        }
    }

    private static boolean handleGeode(FeaturePlacementContext context, Random random, BlockPos pos, Chunk chunk, PlacedFeature placedFeature) {
        Stream<BlockPos> stream = Stream.of(pos);
        PlacedFeatureAccessor placedFeatureAccessor = (PlacedFeatureAccessor)(Object) placedFeature;

        for(PlacementModifier placementModifier : placedFeatureAccessor.getPlacementModifiers()) {
            stream = stream.flatMap(posx -> placementModifier.getPositions(context, random, posx));
        }

        ConfiguredFeature<?, ?> configuredFeature = placedFeatureAccessor.getFeature().value();
        MutableBoolean mutableBoolean = new MutableBoolean();
        stream.forEach(placedPos -> {
            //LOGGER.info("no");
            if(random.nextFloat() >= 0.9) {
                //LOGGER.info("Yes" + chunk.getPos());
                if (configuredFeature.generate(context.getWorld(), context.getChunkGenerator(), random, placedPos)) {
                    mutableBoolean.setTrue();
                }
            }
        });
        return mutableBoolean.isTrue();
    }

    //This function loops through all structureStarts in a chunk, and runs handleStructureStart() on them. Normally in
    // Minecraft instead of running handleStructureStart() on the StructureStart it would generate the StructureStart.
    // The function this is based off is called ChunkGenerator.generateFeatures(). It would also generate features (trees,
    // water pools, things aren't the base terrain, but don't have bounding boxes).
    public static void handleStructures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, ChunkGenerator generator, boolean beforeDelete) {
        ChunkPos chunkPos = chunk.getPos();
        //Check if we should generate features
        if (!SharedConstants.isOutsideGenerationArea(chunkPos)) {
            //Get access to a bunch of private stuff by making a fake this
            ChunkGeneratorInvoker This = ((ChunkGeneratorInvoker) generator);

            //Find where to generate
            ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos, world.getBottomSectionCoord());
            BlockPos minChunkPos = chunkSectionPos.getMinPos();

            //Get the structure registry
            Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            Map<Integer, List<Structure>> stageToStructureMap = registry.stream()
                    .collect(Collectors.groupingBy(structureType -> structureType.getFeatureGenerationStep().ordinal()));

            //Get the structure to place
            List<PlacedFeatureIndexer.IndexedFeatures> list = This.getIndexedFeaturesListSupplier().get();
            int i = list.size();

            //Make a new random
            //FIX: This random needs to equal vanilla
            ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
            long l = chunkRandom.setPopulationSeed(world.getSeed(), minChunkPos.getX(), minChunkPos.getZ());


            //Stuff to do with features
            Set<RegistryEntry<Biome>> set = new ObjectArraySet();
            ChunkPos.stream(chunkSectionPos.toChunkPos(), 1).forEach(chunkPosx -> {
                Chunk chunkx = world.getChunk(chunkPosx.x, chunkPosx.z);

                for(ChunkSection chunkSection : chunkx.getSectionArray()) {
                    chunkSection.getBiomeContainer().forEachValue(set::add);
                }

            });
            set.retainAll(This.getBiomeSource().getBiomes());

            Registry<PlacedFeature> featuresRegistry = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);

            if(!ran) {
                fixRaceCondition(world);
            }

            try {
                int j = Math.max(GenerationStep.Feature.values().length, i);

                for(int k = 0; k < j; ++k) {
                    int m = 0;
                    if (structureAccessor.shouldGenerateStructures()) {
                        for(Structure structure : stageToStructureMap.getOrDefault(k, Collections.emptyList())) {
                            //The chunkrandom is correct to here
                            chunkRandom.setDecoratorSeed(l, m, k);
                            Supplier<String> structureIDSupplier = () -> (String)registry.getKey(structure).map(Object::toString).orElseGet(structure::toString);

                            try {
                                world.setCurrentlyGeneratingStructureName(structureIDSupplier);
                                structureAccessor.getStructureStarts(chunkSectionPos, structure)
                                        //Wrong here
                                        .forEach(start -> {
                                            if(beforeDelete) {
                                                for(Pair<Structure, NineArgumentFunction<Boolean, StructurePiece, StructureWorldAccess, StructureAccessor, ChunkGenerator, Random, BlockBox, ChunkPos, BlockPos, Chunk>> structureFunctionPair : structuresBeforeDelete) {
                                                    if (structure == structureFunctionPair.getLeft()) {
                                                        handleStructureStart(start, world, structureAccessor, generator, chunkRandom, This.getBlockBoxForChunkInvoker(chunk), chunkPos, structureFunctionPair.getRight(), chunk);
                                                        break;
                                                    }
                                                }
                                            } else {
                                                for(Pair<Structure, NineArgumentFunction<Boolean, StructurePiece, StructureWorldAccess, StructureAccessor, ChunkGenerator, Random, BlockBox, ChunkPos, BlockPos, Chunk>> structureFunctionPair : structuresAfterDelete) {
                                                    if (structure == structureFunctionPair.getLeft()) {
                                                        handleStructureStart(start, world, structureAccessor, generator, chunkRandom, This.getBlockBoxForChunkInvoker(chunk), chunkPos, structureFunctionPair.getRight(), chunk);
                                                        break;
                                                    }
                                                }
                                            }
                                            //start.place(world, structureAccessor, generator, chunkRandom, This.getBlockBoxForChunkInvoker(chunk), chunkPos);
                                        });
                            } catch (Exception e) {
                                CrashReport crashReport = CrashReport.create(e, "Feature placement");
                                crashReport.addElement("Feature").add("Description", structureIDSupplier::get);
                                throw new CrashException(crashReport);
                            }

                            ++m;
                        }
                    }

                    //This part is for generating features.
                    if (k < i) {
                        //Get all the features in this stage of generation
                        IntSet intSet = new IntArraySet();

                        for(RegistryEntry<Biome> registryEntry : set) {
                            List<RegistryEntryList<PlacedFeature>> list3 = This.getGenerationSettingsGetter().apply(registryEntry)
                                    .getFeatures();
                            if (k < list3.size()) {
                                RegistryEntryList<PlacedFeature> registryEntryList = list3.get(k);
                                PlacedFeatureIndexer.IndexedFeatures indexedFeatures = list.get(k);
                                registryEntryList.stream()
                                        .map(RegistryEntry::value)
                                        .forEach(placedFeaturex -> intSet.add(indexedFeatures.indexMapping().applyAsInt(placedFeaturex)));
                            }
                        }

                        int n = intSet.size();
                        int[] is = intSet.toIntArray();
                        Arrays.sort(is);
                        PlacedFeatureIndexer.IndexedFeatures indexedFeatures2 = list.get(k);

                        //Loop through all the features we found
                        for(int o = 0; o < n; ++o) {
                            int p = is[o];
                            PlacedFeature placedFeature = indexedFeatures2.features().get(p);
                            Supplier<String> supplier2 = () -> (String)featuresRegistry.getKey(placedFeature).map(Object::toString).orElseGet(placedFeature::toString);
                            chunkRandom.setDecoratorSeed(l, p, k);

                            try {
                                world.setCurrentlyGeneratingStructureName(supplier2);
                                //If it is a geode and it is before deleting place it down
                                if(beforeDelete && placedFeature.feature().matchesId(Identifier.tryParse("minecraft:amethyst_geode"))) {
                                    //LOGGER.info("geode");
                                    //placedFeature.generate(world, generator, chunkRandom, minChunkPos);
                                    handleGeode(new FeaturePlacementContext(world, generator, Optional.of(placedFeature)), chunkRandom, minChunkPos, chunk, placedFeature);
                                    //break;
                                }
                                //placedFeature.generate(world, generator, chunkRandom, minChunkPos);
                            } catch (Exception var30) {
                                CrashReport crashReport2 = CrashReport.create(var30, "Feature placement");
                                crashReport2.addElement("Feature").add("Description", supplier2::get);
                                throw new CrashException(crashReport2);
                            }
                        }
                    }
                }

                world.setCurrentlyGeneratingStructureName(null);
            } catch (Exception var31) {
                CrashReport crashReport3 = CrashReport.create(var31, "Biome decoration");
                crashReport3.addElement("Generation").add("CenterX", chunkPos.x).add("CenterZ", chunkPos.z).add("Seed", l);
                throw new CrashException(crashReport3);
            }
        }
    }

    public static BlockPos getBlockInStructurePiece(StructurePiece piece, int x, int y, int z) {
        StructurePieceAccessor access = (StructurePieceAccessor) piece;
        return new BlockPos(access.invokeApplyXTransform(x, z), access.invokeApplyYTransform(y), access.invokeApplyZTransform(x, z));
    }

    public static void setBlockInStructure(StructurePiece piece, Chunk chunk, BlockState state, int x, int y, int z) {
        StructurePieceAccessor access = (StructurePieceAccessor) piece;
        BlockPos pos = getBlockInStructurePiece(piece, x, y, z);
        if (piece.getBoundingBox().contains(pos)) {
            BlockMirror mirror = access.getMirror();
            if (mirror != BlockMirror.NONE)
                state = state.mirror(mirror);
            BlockRotation rotation = piece.getRotation();
            if (rotation != BlockRotation.NONE)
                state = state.rotate(rotation);

            setBlockInChunk(chunk, pos, state);
        }
    }

    public static void setBlockInChunk(Chunk chunk, BlockPos pos, BlockState state) {
        if (chunk.getPos().equals(new ChunkPos(pos))) {
            chunk.setBlockState(pos, state, false);
        }
    }
}