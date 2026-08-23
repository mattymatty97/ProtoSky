package protosky.mixins.utils;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.interfaces.SectionOfChunk;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin {
    @Inject(method = "getSection", at = @At("RETURN"))
    private void trackSection(CallbackInfoReturnable<LevelChunkSection> cir, @Local(argsOnly = true, name = "i") int yIndex) {
        LevelChunkSection section = cir.getReturnValue();
        if (section != null) {
            ((SectionOfChunk) section).protoSky$setChunk((ChunkAccess) (Object) this);
            ((SectionOfChunk) section).protoSky$setSectionIndex(yIndex);
            ((SectionOfChunk) section).protoSky$setYOffset(((ChunkAccess) (Object) this).getSectionYFromSectionIndex(yIndex) * 16);
        }
    }
}
