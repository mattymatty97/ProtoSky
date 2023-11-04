package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ProtoSkyMod;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @WrapOperation(method = "moveToSpawn", at=@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getSpawnPos()Lnet/minecraft/util/math/BlockPos;"))
    BlockPos getWorldSpawn(ServerWorld instance, Operation<BlockPos> original){
        BlockPos forceSpawn = ProtoSkyMod.spawnInfo.spawnPos();
        return forceSpawn != null ? forceSpawn : original.call(instance);
    }

}
