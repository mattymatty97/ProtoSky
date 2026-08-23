package protosky;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class Debug {
    public static Map<String, AttemptCounter> attemptMap = new HashMap<>();
    public static boolean anyAttempt = false;

    public static boolean preventSave = false;

    public static Block chunkOriginBlock = null;

    public record AttemptCounter(Set<BlockPos> total, Set<BlockPos> graced, Set<BlockPos> vanilla,
                                 Set<BlockPos> generated) {
        public AttemptCounter(Set<BlockPos> total, Set<BlockPos> graced, Set<BlockPos> vanilla, Set<BlockPos> generated) {
            this.total = total;
            this.graced = graced;
            this.vanilla = vanilla;
            this.generated = generated;
        }

        public AttemptCounter() {
            this(Collections.synchronizedSet(new HashSet<>()), Collections.synchronizedSet(new HashSet<>()), Collections.synchronizedSet(new HashSet<>()), Collections.synchronizedSet(new HashSet<>()));
        }
    }

}
