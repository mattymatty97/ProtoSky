package protosky.mixins;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.WorldGenUtils;
import protosky.stuctures.PillarHelper;
import protosky.stuctures.StructureHelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static net.minecraft.world.chunk.ChunkStatus.*;
import static protosky.ProtoSkySettings.LOGGER;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {
    @Shadow
    @Final
    private ChunkStatus previous;


    //This is where features's and structures's blocks should get placed, now it's where the structures and features ProtoSky leaves get placed.
    private static void FEATURES(ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk/*, CallbackInfo ci*/) {
        Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE));
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 1);
        //This would normally generate structures, the blocks, not the bounding boxes.
        //generator.generateFeatures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion));
        //Blender.tickLeavesAndFluids(chunkRegion, chunk);

        //I couldn't figure out how to generate the shulkers and elytras without also generating the blocks, so we generate the whole end city and delete the blocks
        if (world.getRegistryKey() == World.END) {
            //This generates all the structures
            StructureHelper.handleStructures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion), generator, true);
            //Delete all the terrain and end cities. I couldn't figure out how to generate just the shulkers and elytra, so I resorted to just generating the whole thing and deleting the blocks.
            WorldGenUtils.deleteBlocks(chunk, world);
            //Generate the end pillars. This is its own thing and not in handleStructures() because pillars are features not structures.
            PillarHelper.generate(world, chunk);

            //Do it the other way around when generating the ow as to leave the end portal frames.
        } else {
            //generator.generateFeatures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion));
            //Blender.tickLeavesAndFluids(chunkRegion, chunk);

            //We need handle some structures before we delete blocks because they rely on blocks being there.
            StructureHelper.handleStructures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion), generator, true);
        }
    }

    //My own status that I couldn't make a stage for it working
/*    private static void afterFeaturesFunction(ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk) {
        LOGGER.info("After features");
    }
    private static ChunkStatus AFTER_FEATURES;*/

    //This injects into the function that would normally register the statues to registry and changes some of the vanilla values
    // I do it this way because I couldn't figure out how to change static fields. I know about <clinit>, but the statuses have
    // to be initialized after any previous stage and before any stages that declare it as previous. This was easier.
    @Inject(method = "register(Ljava/lang/String;Lnet/minecraft/world/chunk/ChunkStatus;IZLjava/util/EnumSet;Lnet/minecraft/world/chunk/ChunkStatus$ChunkType;Lnet/minecraft/world/chunk/ChunkStatus$GenerationTask;Lnet/minecraft/world/chunk/ChunkStatus$LoadTask;)Lnet/minecraft/world/chunk/ChunkStatus;",
            at = @At("HEAD"), cancellable = true)
    private static void register(String id,
                                 ChunkStatus previous,
                                 int taskMargin,
                                 boolean shouldAlwaysUpgrade,
                                 EnumSet<Heightmap.Type> heightMapTypes,
                                 ChunkStatus.ChunkType chunkType,
                                 ChunkStatus.GenerationTask generationTask,
                                 ChunkStatus.LoadTask loadTask,
                                 CallbackInfoReturnable<ChunkStatus> cir
    ) {
        //Task margin will request that amount of chunks around the one we are generating to at least be started. If they aren't the game will crash

        //This modifies the initialize_light status to have a different task margin and run some code before the normal code
        // that should be in its own stage, but I couldn't create one, so I use this one.
        if (id.equals("initialize_light")) {
            //AFTER_FEATURES = ChunkStatusInvoker.invokeRegister("after_features", ChunkStatus.FEATURES, 8, POST_CARVER_HEIGHTMAPS, ChunkStatus.ChunkType.PROTOCHUNK, ChunkStatusMixin::afterFeaturesFunction);
            cir.setReturnValue(Registry.register(
                    Registries.CHUNK_STATUS,
                    id,
                    new ChunkStatus(
                            //AFTER_FEATURES,
                            previous,
                            8,
                            shouldAlwaysUpgrade,
                            heightMapTypes,
                            chunkType,
                            (
                                    ChunkStatus targetStatus,
                                    Executor executor,
                                    ServerWorld world,
                                    ChunkGenerator generator,
                                    StructureTemplateManager structureTemplateManager,
                                    ServerLightingProvider lightingProvider,
                                    Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter,
                                    List<Chunk> chunks,
                                    Chunk chunk
                            ) -> {
                                //This deletes all the blocks in a later stage than features because there were some issues where
                                // Geodes placed in and deleted in the same stage would not get deleted if they went over chunk borders
                                //It also runs and handlers that need to be run after delete
                                if (world.getRegistryKey() != World.END) {
                                    WorldGenUtils.deleteBlocks(chunk, world);

                                    ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 1);
                                    StructureHelper.handleStructures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion), generator, false);
                                }

                                //We need to move the heightmaps down to the bottom of the world after structures have been generated because some rely on the heightmap to move.
                                //This used to be in the unused 'HEIGHTMAPS' status, but in 1.20 this was removed. Now we're using INITIALIZE_LIGHT.
                                //This gets done here not above in FEATURES because there are multiple threads that generate features. One thread
                                // may place a structure in a chunk then move the heightmap down to y=-64 when a second structure in the chunk on
                                // a different thread still needs to be generated and requires the 'correct' heightmap
                                WorldGenUtils.genHeightMaps(chunk);

                                //Run the normal INITIALIZE_LIGHT code because we still want that to work. All we want to do is make some code run before it
                                return generationTask.doWork(targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk);
                            },
                            loadTask)
            ));

            //This overrides the vanilla code of the features status to use my own code seen above
        } else if (id.equals("features")) {
            cir.setReturnValue(Registry.register(
                    Registries.CHUNK_STATUS,
                    id,
                    new ChunkStatus(
                            previous,
                            taskMargin,
                            shouldAlwaysUpgrade,
                            heightMapTypes,
                            chunkType,
                            (ChunkStatus.SimpleGenerationTask) ChunkStatusMixin::FEATURES,
                            loadTask
                    )
            ));

            //This stops mobs from spawning as we don't want to see random mobs falling from structures that didn't get placed
        } else if (id.equals("spawn")) {
            cir.setReturnValue(Registry.register(
                    Registries.CHUNK_STATUS,
                    id,
                    new ChunkStatus(
                            previous,
                            taskMargin,
                            shouldAlwaysUpgrade,
                            heightMapTypes,
                            chunkType,
                            (ChunkStatus.SimpleGenerationTask) (
                                    ChunkStatus targetStatus,
                                    ServerWorld world,
                                    ChunkGenerator generator,
                                    List<Chunk> chunks,
                                    Chunk chunk
                            ) -> {
                            },
                            loadTask
                    )
            ));

            //Just pass stuff through by default
        } else {
            cir.setReturnValue(Registry.register(
                    Registries.CHUNK_STATUS,
                    id,
                    new ChunkStatus(
                            previous,
                            taskMargin,
                            shouldAlwaysUpgrade,
                            heightMapTypes,
                            chunkType,
                            generationTask,
                            loadTask
                    )
            ));
        }
    }

    //This was my attempt to add my own stage, but it results in a stackoverflow whenever it is used to add a custom stage
    // or duplicate any status that is not STRUCTURE_STARTS. Add -Xss400K to your jvm args to make the stack fit in the 1024 line max stack trace length
    /*@Inject(method = "<clinit>", at = @At("TAIL"), cancellable = false)
    private static void clinit(CallbackInfo ci) {
        DISTANCE_TO_STATUS = new ArrayList<ChunkStatus>();
        DISTANCE_TO_STATUS.add(FULL);
        DISTANCE_TO_STATUS.add(INITIALIZE_LIGHT);
        DISTANCE_TO_STATUS.add(AFTER_FEATURES);
        DISTANCE_TO_STATUS.add(CARVERS);
        DISTANCE_TO_STATUS.add(BIOMES);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(STRUCTURE_STARTS);
        DISTANCE_TO_STATUS.add(EMPTY);
    }*/
}

