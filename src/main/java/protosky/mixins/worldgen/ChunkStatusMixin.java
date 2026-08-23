package protosky.mixins.worldgen;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.utils.WorldGenUtils;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {

    @Inject(method = "method_51376(Lnet/minecraft/world/level/chunk/ChunkStatus;Ljava/util/concurrent/Executor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/server/level/ThreadedLevelLightEngine;Ljava/util/function/Function;Ljava/util/List;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private static void on_initialize_light(CallbackInfoReturnable<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> cir,
                                            @Local(argsOnly = true) ServerLevel world,
                                            @Local(argsOnly = true) ChunkAccess chunk
    ) {
        //if the world is in the ignored list, do nothing and let vanilla code run
        if (ProtoSkyMod.ignoredWorlds.contains(world.dimension())) return;

        WorldGenUtils.deleteBlocks(chunk, world);
        WorldGenUtils.clearEntities((ProtoChunk) chunk, world);

        WorldGenUtils.resetHeightMaps(chunk);

        WorldGenUtils.restoreBlocks(chunk, world);
        WorldGenUtils.restoreEntities((ProtoChunk) chunk, world);
    }

    @Inject(method = "method_17033(Lnet/minecraft/world/level/chunk/ChunkStatus;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Ljava/util/List;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("HEAD"), cancellable = true)
    private static void on_spawn(CallbackInfo ci,
                                 @Local(argsOnly = true) ServerLevel world
    ) {
        //if the world is in the ignored list, do nothing and let vanilla code run
        if (ProtoSkyMod.ignoredWorlds.contains(world.dimension())) return;

        //otherwise skip the spawn step (output the chunk as is)
        ci.cancel();
    }
}

