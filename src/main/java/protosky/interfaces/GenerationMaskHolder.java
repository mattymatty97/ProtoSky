package protosky.interfaces;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import protosky.interfaces.FeatureWorldMask;

public interface GenerationMaskHolder {

    FeatureWorldMask protoSky$getMask();

    void protoSky$updateMask(String name, BlockPos origin);
}
