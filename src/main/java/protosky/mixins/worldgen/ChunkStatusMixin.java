package protosky.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.Heightmap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.utils.WorldGenUtils;

import java.util.EnumSet;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusMixin {

    @WrapOperation(method = "<clinit>",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/ChunkStatus;POST_CARVER_HEIGHTMAPS:Ljava/util/EnumSet;", opcode = Opcodes.PUTSTATIC)
    )
    private static void patch_heightmaps(EnumSet<Heightmap.Type> types, Operation<Void> original) {
        types.add(Heightmap.Type.PROTO_SKY_VANILLA_WORLD_SURFACE);
        types.add(Heightmap.Type.PROTO_SKY_VANILLA_OCEAN_FLOOR);
        original.call(types);
    }

    @Inject(method = "method_51376(Lnet/minecraft/world/chunk/ChunkStatus;Ljava/util/concurrent/Executor;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureTemplateManager;Lnet/minecraft/server/world/ServerLightingProvider;Ljava/util/function/Function;Ljava/util/List;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private static void on_initialize_light(CallbackInfoReturnable<Either<Chunk, ChunkHolder.Unloaded>> cir,
                                            @Local(argsOnly = true) ServerWorld world,
                                            @Local(argsOnly = true) Chunk chunk
    ) {
        //if the world is in the ignored list, do nothing and let vanilla code run
        if (ProtoSkyMod.ignoredWorlds.contains(world.getRegistryKey())) return;

        WorldGenUtils.fillCustomHeightmaps(chunk);

        WorldGenUtils.deleteBlocks(chunk, world);
        WorldGenUtils.clearEntities((ProtoChunk) chunk, world);

        WorldGenUtils.resetHeightMaps(chunk);

        WorldGenUtils.restoreBlocks(chunk, world);
        WorldGenUtils.restoreEntities((ProtoChunk) chunk, world);
    }

    // TODO(Ravel): target method method_17033 with the signature not found
    @Inject(method = "method_17033(Lnet/minecraft/world/chunk/ChunkStatus;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Ljava/util/List;Lnet/minecraft/world/chunk/Chunk;)V", at = @At("HEAD"), cancellable = true)
    private static void on_spawn(CallbackInfo ci,
                                 @Local(argsOnly = true) ServerWorld world
    ) {
        //if the world is in the ignored list, do nothing and let vanilla code run
        if (ProtoSkyMod.ignoredWorlds.contains(world.getRegistryKey())) return;

        //otherwise skip the spawn step (output the chunk as is)
        ci.cancel();
    }
}

