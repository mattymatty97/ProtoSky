package protosky.interfaces;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;

public interface FeatureWorldMask{
    default boolean canGenerate(Double value){
        return false;
    }
    default boolean canPlace(LocalRef<BlockState> blockState, Double value){
        return false;
    }
    default boolean canSpawn(LocalRef<Entity> entity, Double value){
        return false;
    }

    default boolean isReplaceable(){ return false; }

}
