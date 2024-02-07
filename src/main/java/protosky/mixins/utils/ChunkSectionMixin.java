package protosky.mixins.utils;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.SectionOfChunk;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Supplier;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements SectionOfChunk {
    @Unique
    private WeakReference<Chunk> chunk = new WeakReference<>(null);
    @Unique
    private int index = 0;

    @Unique
    private int yOffset;

    @Override
    public Chunk protoSky$getChunk() {
        return this.chunk.get();
    }

    @Override
    public int protoSky$getSectionIndex() {
        return this.index;
    }

    @Override
    public void protoSky$setChunk(Chunk chunk) {
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
    }    @Override
    public void protoSky$setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

}
