package protosky.interfaces;

import net.minecraft.world.chunk.Chunk;

import java.util.function.Supplier;

public interface SectionOfChunk {
    Chunk protoSky$getChunk();

    int protoSky$getSectionIndex();

    void protoSky$setChunk(Chunk chunk);

    void protoSky$setSectionIndex(int index);
    int protoSky$getYOffset(Supplier<Integer> getter);
}
