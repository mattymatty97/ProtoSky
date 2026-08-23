package protosky.interfaces;

import net.minecraft.world.level.chunk.ChunkStatus;

public interface RetrogenHolder {
    ChunkStatus protoSky$getPreviousStatus();

    void protoSky$setPreviousStatus(ChunkStatus status);
}
