package protosky;

import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.ChunkRegion;

public class ThreadLocals {
    public static ThreadLocal<ProtoSkySpawn> forcedSpawn = new ThreadLocal<>();
    public static ThreadLocal<ChunkRandom> graceRandom = new ThreadLocal<>();

    public static ThreadLocal<ChunkRegion> currentRegion = new ThreadLocal<>();
}
