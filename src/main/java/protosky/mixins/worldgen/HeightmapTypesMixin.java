package protosky.mixins.worldgen;

import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Predicate;

@Mixin(Heightmap.Type.class)
public enum HeightmapTypesMixin {
    PROTO_SKY_VANILLA_WORLD_SURFACE("VANILLA_WORLD_SURFACE", Heightmap.Purpose.WORLDGEN, (state) -> false),
    PROTO_SKY_VANILLA_OCEAN_FLOOR("VANILLA_OCEAN_FLOOR", Heightmap.Purpose.WORLDGEN, (state) -> false);

    @Shadow
    HeightmapTypesMixin(String string2, Heightmap.Purpose usage, Predicate<BlockState> predicate) {
    }
}
