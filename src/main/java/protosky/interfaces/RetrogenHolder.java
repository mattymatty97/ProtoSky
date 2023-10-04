package protosky.interfaces;

import net.minecraft.world.chunk.ChunkStatus;

public interface RetrogenHolder {
    boolean protoSky$usesBelowZeroRetrogen();
    ChunkStatus protoSky$getPreviousStatus();

    void protoSky$setBelowZeroRetrogen(boolean value);
    void protoSky$setPreviousStatus(ChunkStatus status);
}
