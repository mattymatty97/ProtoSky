package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.ProtoSkySpawn;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "createPlayer", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/world/ServerWorld;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/server/network/ServerPlayerEntity;", shift = At.Shift.BEFORE))
    void setCreationVariables(GameProfile profile, CallbackInfoReturnable<ServerPlayerEntity> cir){
        ProtoSkyMod.forcedSpawn.set(ProtoSkyMod.spawnInfo);
    }
    @Inject(method = "createPlayer", at = @At(value = "RETURN"))
    void releaseCreationVariables(GameProfile profile, CallbackInfoReturnable<ServerPlayerEntity> cir){
        ProtoSkyMod.forcedSpawn.remove();
    }

    @WrapOperation(method = "createPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
    ServerWorld setCreationWorld(MinecraftServer instance, Operation<ServerWorld> original){
        ProtoSkySpawn forcedSpawn = ProtoSkyMod.forcedSpawn.get();
        if (forcedSpawn != null) {
            RegistryKey<World> forcedWorld = forcedSpawn.spawnWorld();
            if (forcedWorld != null) {
                ServerWorld world = instance.getWorld(forcedWorld);
                if (world != null) {
                    return world;
                } else {
                    ProtoSkyMod.LOGGER.error("Unknown spawn world: {}", forcedWorld.getValue().toString());
                }
            }
        }
        return original.call(instance);
    }


    @Inject(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;", shift = At.Shift.BEFORE))
    void setNewSpawnWorld(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci, @Local(ordinal = 0) NbtCompound nbtCompound, @Local(ordinal = 0) LocalRef<RegistryKey<World>> registryKey){
        if (nbtCompound == null){
            ProtoSkyMod.forcedSpawn.set(ProtoSkyMod.spawnInfo);
            RegistryKey<World> forcedWorld = ProtoSkyMod.spawnInfo.spawnWorld();
            if (forcedWorld != null)
                registryKey.set(forcedWorld);
        }
    }

    @Inject(method = "onPlayerConnect", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    void unsetNewSpawnWorld(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci){
        ProtoSkyMod.forcedSpawn.remove();
    }

    @WrapOperation(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
    ServerWorld setRespawnWorld(MinecraftServer instance, Operation<ServerWorld> original){
        ProtoSkyMod.forcedSpawn.set(ProtoSkyMod.spawnInfo);
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

    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    void resetForcedSpawn(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir){
        ProtoSkyMod.forcedSpawn.remove();
    }
}
