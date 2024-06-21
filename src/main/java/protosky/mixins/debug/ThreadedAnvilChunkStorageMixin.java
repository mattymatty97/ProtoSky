package protosky.mixins.debug;

import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.Debug;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {

    //TODO: remove before release
    //prevent saving chunks to be able to test on the same world multiple times
    @Inject(method = "save(Lnet/minecraft/world/chunk/Chunk;)Z", at=@At("HEAD"), cancellable = true)
    private void save(Chunk chunk, CallbackInfoReturnable<Boolean> cir){
        if (Debug.preventSave)
            cir.setReturnValue(true);
    }
}
