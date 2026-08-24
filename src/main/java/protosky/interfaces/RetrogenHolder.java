package protosky.interfaces;

import net.minecraft.world.chunk.ChunkStatus;

public interface RetrogenHolder {
    ChunkStatus protoSky$getPreviousStatus();

    void protoSky$setPreviousStatus(ChunkStatus status);
}
