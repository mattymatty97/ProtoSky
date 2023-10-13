package protosky.interfaces;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import protosky.ProtoSkyMod;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public interface FeatureWorldMask {
    boolean hasGraces(Random random);
    boolean canPlace(LocalRef<BlockState> blockState, Random random, Map<ProtoSkyMod.GraceConfig.SubConfig,AtomicInteger> map);
    boolean canSpawn(LocalRef<Entity> entity, Random random, Map<ProtoSkyMod.GraceConfig.SubConfig,AtomicInteger> map);
}
