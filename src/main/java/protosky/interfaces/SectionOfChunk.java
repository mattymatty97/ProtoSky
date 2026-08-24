package protosky.interfaces;

import net.minecraft.world.chunk.Chunk;

public interface SectionOfChunk {
    Chunk protoSky$getChunk();

    int protoSky$getSectionIndex();

    void protoSky$setChunk(Chunk chunk);

    void protoSky$setSectionIndex(int index);

    int protoSky$getYOffset();

    void protoSky$setYOffset(int yOffset);
}
