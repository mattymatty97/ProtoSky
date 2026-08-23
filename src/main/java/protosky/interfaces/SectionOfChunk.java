package protosky.interfaces;

import net.minecraft.world.level.chunk.ChunkAccess;

public interface SectionOfChunk {
    ChunkAccess protoSky$getChunk();

    int protoSky$getSectionIndex();

    void protoSky$setChunk(ChunkAccess chunk);

    void protoSky$setSectionIndex(int index);

    int protoSky$getYOffset();

    void protoSky$setYOffset(int yOffset);
}
