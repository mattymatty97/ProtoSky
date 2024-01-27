package protosky.mixins.utils;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.SectionOfChunk;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements SectionOfChunk {
    @Unique
    private Chunk chunk = null;
    @Unique
    private int index = 0;

    @Override
    public Chunk protoSky$getChunk() {
        return this.chunk;
    }

    @Override
    public int protoSky$getSectionIndex() {
        return this.index;
    }

    @Override
    public void protoSky$setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public void protoSky$setSectionIndex(int index) {
        this.index = index;
    }
}
