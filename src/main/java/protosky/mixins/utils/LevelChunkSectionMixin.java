package protosky.mixins.utils;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.SectionOfChunk;

import java.lang.ref.WeakReference;

@Mixin(LevelChunkSection.class)
public class LevelChunkSectionMixin implements SectionOfChunk {
    @Unique
    private WeakReference<ChunkAccess> chunk = new WeakReference<>(null);
    @Unique
    private int index = 0;

    @Unique
    private int yOffset;

    @Override
    public ChunkAccess protoSky$getChunk() {
        return this.chunk.get();
    }

    @Override
    public int protoSky$getSectionIndex() {
        return this.index;
    }

    @Override
    public void protoSky$setChunk(ChunkAccess chunk) {
        if (this.chunk.get() != chunk) {
            this.chunk = new WeakReference<>(chunk);
        }
    }

    @Override
    public void protoSky$setSectionIndex(int index) {
        if (this.index != index) {
            this.index = index;
        }
    }

    @Override
    public int protoSky$getYOffset() {
        return this.yOffset;
    }

    @Override
    public void protoSky$setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

}
