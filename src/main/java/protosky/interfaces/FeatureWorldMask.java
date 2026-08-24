package protosky.interfaces;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.Entity;
import net.minecraft.block.BlockState;

public interface FeatureWorldMask {
    default boolean canGenerate(Double value) {
        return false;
    }

    default boolean canPlace(LocalRef<BlockState> blockState, Double value) {
        return false;
    }

    default boolean canSpawn(LocalRef<Entity> entity, Double value) {
        return false;
    }

    default boolean isReplaceable() {
        return false;
    }

}
