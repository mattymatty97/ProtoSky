package protosky.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;

public interface GenerationMaskHolder {

    FeatureWorldMask protoSky$getMask();

    void protoSky$updateRollbacks(BlockPos pos, boolean remove);

    void protoSky$setMask(ResourceKey<?>[] keys, BlockPos origin);

    void protoSky$unsetMask(ResourceKey<?>[] keys, BlockPos origin);

    void protoSky$logMask(boolean wasGenerated);
}
