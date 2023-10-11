package protosky.mixins.worldgen.below_zero;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.RetrogenHolder;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements RetrogenHolder{
    @Unique
    ChunkStatus previousStatus = ChunkStatus.EMPTY;

    @Override
    public ChunkStatus protoSky$getPreviousStatus() {
        return previousStatus;
    }

    @Override
    public void protoSky$setPreviousStatus(ChunkStatus status) {
        previousStatus = status;
    }
}
