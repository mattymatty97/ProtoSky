package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ThreadLocals;
import protosky.datapack.ProtoSkySpawn;

@Mixin(Level.class)
public class LevelMixin {

    @WrapOperation(method = "getSharedSpawnPos", at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;"))
    private BlockPos forcedSpawnPos(int posx, int posy, int posz, Operation<BlockPos> original) {
        ProtoSkySpawn forcedSpawn = ThreadLocals.forcedSpawn.get();
        if (forcedSpawn != null && forcedSpawn.spawnPos() != null) {
            BlockPos forced = forcedSpawn.spawnPos();
            return original.call(forced.getX(), forced.getY(), forced.getZ());
        }
        return original.call(posx, posy, posz);
    }


}
