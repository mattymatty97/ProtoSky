package protosky.interfaces;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;

public interface GenerationMaskHolder {

    FeatureWorldMask protoSky$getMask();

    void protoSky$updateRollbacks(BlockPos pos, boolean remove);

    void protoSky$setMask(RegistryKey<?>[] keys, BlockPos origin);
    void protoSky$unsetMask(RegistryKey<?>[] keys, BlockPos origin);
    void protoSky$logMask(boolean wasGenerated);
}
