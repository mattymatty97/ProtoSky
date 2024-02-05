package protosky;

import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import protosky.datapack.ProtoSkySpawn;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadLocals {
    public static ThreadLocal<ProtoSkySpawn> forcedSpawn = new ThreadLocal<>();
    public static ThreadLocal<ChunkRandom> graceRandom = new ThreadLocal<>();

    public static ThreadLocal<ChunkRegion> currentRegion = new ThreadLocal<>();

    public static final ThreadLocal<Queue<Structure>> currentStructure = ThreadLocal.withInitial(ConcurrentLinkedQueue::new);

    public static final ThreadLocal<Queue<PlacedFeature>> currentPlacedFeature = ThreadLocal.withInitial(ConcurrentLinkedQueue::new);
}
