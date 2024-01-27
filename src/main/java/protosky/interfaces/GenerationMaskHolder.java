package protosky.interfaces;

import net.minecraft.util.math.BlockPos;

public interface GenerationMaskHolder {

    FeatureWorldMask protoSky$getMask();

    void protoSky$updateRollbacks(BlockPos pos, boolean remove);

    void protoSky$updateMask(String[] names, BlockPos origin);
    void protoSky$logMask(String[] names, BlockPos origin, boolean wasGenerated);
}
