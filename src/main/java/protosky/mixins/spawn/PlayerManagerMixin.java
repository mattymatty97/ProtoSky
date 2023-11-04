package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ProtoSkyMod;
@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @WrapOperation(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
    ServerWorld setRespawnWorld(MinecraftServer instance, Operation<ServerWorld> original){
        RegistryKey<World> forcedWorld = ProtoSkyMod.spawnInfo.spawnWorld();
        if (forcedWorld != null){
            ServerWorld world = instance.getWorld(forcedWorld);
            if (world != null){
                return world;
            }else{
                ProtoSkyMod.LOGGER.error("Unknown spawn world: {}", forcedWorld.getValue().toString());
            }
        }
        return original.call(instance);
    }
}
