package protosky.mixins.accessors;

import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccessor {
    @Accessor("lightingProvider")
    LightingProvider getLightingProvider();
}
