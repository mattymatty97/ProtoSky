package protosky.mixins.worldgen;

import com.mojang.datafixers.util.Either;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.WorldGenUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {
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
        if (id.equals("initialize_light")) {
            cir.setReturnValue(Registry.register(
                    Registries.CHUNK_STATUS,
                    id,
                    new ChunkStatus(
                            previous,
                            taskMargin,
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

                                //if the world is in the ignored list do nothing and let vanilla code run
                                if (!ProtoSkyMod.ignoredWorlds.contains(world.getRegistryKey())) {
                                    WorldGenUtils.deleteBlocks(chunk, world);
                                    WorldGenUtils.clearEntities((ProtoChunk) chunk, world);

                                    WorldGenUtils.restoreBlocks(chunk, world);
                                    WorldGenUtils.resetHeightMaps(chunk);
                                    WorldGenUtils.restoreEntities((ProtoChunk) chunk, world);
                                }

                                //Run the normal INITIALIZE_LIGHT code because we still want that to work. All we want to do is make some code run before it
                                return generationTask.doWork(targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk);
                            },
                            loadTask)
            ));

        }
        else if (id.equals("spawn")) {
            cir.setReturnValue(Registry.register(
                    Registries.CHUNK_STATUS,
                    id,
                    new ChunkStatus(
                            previous,
                            taskMargin,
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

                                if (ProtoSkyMod.ignoredWorlds.contains(world.getRegistryKey())) {
                                    //if world is in the ignored list Run the normal code
                                    return generationTask.doWork(targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk);
                                }else {
                                    //otherwise skip the spawn step ( output the chunk as is )
                                    return CompletableFuture.completedFuture(Either.left(chunk));
                                }

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
}

