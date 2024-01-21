package protosky.mixins.accessors;

import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(ChunkRegion.class)
public interface ChunkRegionAccessor {
    @Accessor
    Supplier<String> getCurrentlyGeneratingStructureName();
}
