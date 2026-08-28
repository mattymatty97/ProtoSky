package protosky.mixins.worldgen;

import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.MutabilityHolder;

import java.util.function.Predicate;

@Mixin(Heightmap.Type.class)
public enum HeightmapTypesMixin implements MutabilityHolder {
    PROTO_SKY_VANILLA_WORLD_SURFACE("VANILLA_WORLD_SURFACE", Heightmap.Purpose.LIVE_WORLD, (state) -> false){
        @Override
        public boolean protoSky$isMutable() {
            return false;
        }
    },
    PROTO_SKY_VANILLA_OCEAN_FLOOR("VANILLA_OCEAN_FLOOR", Heightmap.Purpose.LIVE_WORLD, (state) -> false){
        @Override
        public boolean protoSky$isMutable() {
            return false;
        }
    };

    @Shadow
    HeightmapTypesMixin(String string2, Heightmap.Purpose usage, Predicate<BlockState> predicate) {
    }
}
