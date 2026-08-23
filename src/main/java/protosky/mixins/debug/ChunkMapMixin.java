package protosky.mixins.debug;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.Debug;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    //TODO: remove before release
    //prevent saving chunks to be able to test on the same world multiple times
    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("HEAD"), cancellable = true)
    private void save(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true, name = "chunkAccess") ChunkAccess chunk) {
        if (Debug.preventSave) {
            chunk.setUnsaved(false);
            cir.setReturnValue(true);
        }
    }
}
