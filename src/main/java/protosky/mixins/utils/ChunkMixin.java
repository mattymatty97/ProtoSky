package protosky.mixins.utils;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.interfaces.SectionOfChunk;

@Mixin(Chunk.class)
public class ChunkMixin {
    @Inject(method = "getSection", at = @At("RETURN"))
    private void trackSection(int yIndex, CallbackInfoReturnable<ChunkSection> cir){
        ChunkSection section = cir.getReturnValue();
        if (section != null){
            ((SectionOfChunk)section).protoSky$setChunk((Chunk)(Object)this);
            ((SectionOfChunk)section).protoSky$setSectionIndex(yIndex);
            ((SectionOfChunk)section).protoSky$setYOffset(((Chunk)(Object)this).sectionIndexToCoord(yIndex) * 16);
        }
    }
}
