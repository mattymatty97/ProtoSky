package protosky;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import protosky.datapack.ProtoSkySpawn;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadLocals {
    public static ThreadLocal<ProtoSkySpawn> forcedSpawn = new ThreadLocal<>();
    public static ThreadLocal<WorldgenRandom> graceRandom = new ThreadLocal<>();

    public static ThreadLocal<WorldGenRegion> currentRegion = new ThreadLocal<>();

    public static final ThreadLocal<Queue<Structure>> currentStructure = ThreadLocal.withInitial(ConcurrentLinkedQueue::new);

    public static final ThreadLocal<Queue<PlacedFeature>> currentPlacedFeature = ThreadLocal.withInitial(ConcurrentLinkedQueue::new);
}
