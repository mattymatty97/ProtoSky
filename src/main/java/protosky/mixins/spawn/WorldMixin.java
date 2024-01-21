package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ProtoSkyMod;
import protosky.ProtoSkySpawn;

@Mixin(World.class)
public class WorldMixin {

    @WrapOperation(method = "getSpawnPos", at = @At(value = "NEW", target = "(III)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos forcedSpawnPos(int posx, int posy, int posz, Operation<BlockPos> original){
        ProtoSkySpawn forcedSpawn = ProtoSkyMod.forcedSpawn.get();
        if (forcedSpawn != null && forcedSpawn.spawnPos() != null) {
            BlockPos forced = forcedSpawn.spawnPos();
            return original.call(forced.getX(),forced.getY(),forced.getZ());
        }
        return original.call(posx,posy,posz);
    }


}
