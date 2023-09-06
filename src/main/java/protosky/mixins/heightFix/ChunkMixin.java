package protosky.mixins.heightFix;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.RetrogenHolder;

@Mixin(Chunk.class)
public class ChunkMixin implements RetrogenHolder{
    @Unique
    boolean hadBelowZeroRetrogen = false;

    @Override
    public boolean wasBelowZeroRetrogen(){
        return hadBelowZeroRetrogen;
    }

    @Override
    public void setBelowZeroRetrogen(boolean value) {
        hadBelowZeroRetrogen = value;
    }
}
