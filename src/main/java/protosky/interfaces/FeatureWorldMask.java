package protosky.interfaces;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import protosky.ProtoSkyMod;

public interface FeatureWorldMask {
    boolean hasGraces(Double value);
    boolean canPlace(LocalRef<BlockState> blockState, Double value);
    boolean canSpawn(LocalRef<Entity> entity, Double value);
}
