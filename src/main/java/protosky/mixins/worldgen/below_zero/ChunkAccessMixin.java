package protosky.mixins.worldgen.below_zero;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.RetrogenHolder;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements RetrogenHolder {
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
