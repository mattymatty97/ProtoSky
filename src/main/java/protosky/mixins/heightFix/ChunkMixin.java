package protosky.mixins.heightFix;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.RetrogenHolder;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements RetrogenHolder{
    @Shadow public abstract ChunkStatus getStatus();

    @Unique
    boolean hadBelowZeroRetrogen = false;

    @Unique
    ChunkStatus previousStatus = ChunkStatus.EMPTY;

    @Override
    public boolean protoSky$usesBelowZeroRetrogen(){
        return hadBelowZeroRetrogen;
    }

    @Override
    public ChunkStatus protoSky$getPreviousStatus() {
        return previousStatus;
    }

    @Override
    public void protoSky$setBelowZeroRetrogen(boolean value) {
        hadBelowZeroRetrogen = value;
    }

    @Override
    public void protoSky$setPreviousStatus(ChunkStatus status) {
        previousStatus = status;
    }
}
