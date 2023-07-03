package protosky.mixins;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStatus.class)
public interface ChunkStatusInvoker {
    @Invoker
    static ChunkStatus invokeRegister(
            String id,
            @Nullable ChunkStatus previous,
            int taskMargin,
            EnumSet<Heightmap.Type> heightMapTypes,
            ChunkStatus.ChunkType chunkType,
            ChunkStatus.SimpleGenerationTask task
    ) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> invokeGetInitializeLightingFuture(ServerLightingProvider lightingProvider, Chunk chunk) {
        throw new UnsupportedOperationException();
    }
}
