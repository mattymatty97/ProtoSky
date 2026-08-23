package protosky.mixins.worldgen;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.function.Predicate;

@Mixin(Heightmap.Types.class)
public enum HeightmapTypesMixin {
    PROTO_SKY_VANILLA_WORLD_SURFACE("VANILLA_WORLD_SURFACE", Heightmap.Usage.WORLDGEN, (state) -> false),
    PROTO_SKY_VANILLA_OCEAN_FLOOR("VANILLA_OCEAN_FLOOR", Heightmap.Usage.WORLDGEN, (state) -> false);

    @Shadow
    HeightmapTypesMixin(String string2, Heightmap.Usage usage, Predicate<BlockState> predicate) {
    }
}
