package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.ThreadLocals;
import protosky.datapack.ProtoSkySpawn;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "getPlayerForLogin", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/server/level/ServerPlayer;", shift = At.Shift.BEFORE))
    void setCreationVariables(CallbackInfoReturnable<ServerPlayer> cir) {
        ThreadLocals.forcedSpawn.set(ProtoSkyMod.spawnInfo);
    }

    @Inject(method = "getPlayerForLogin", at = @At(value = "RETURN"))
    void releaseCreationVariables(CallbackInfoReturnable<ServerPlayer> cir) {
        ThreadLocals.forcedSpawn.remove();
    }

    @WrapOperation(method = "getPlayerForLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    ServerLevel setCreationWorld(MinecraftServer instance, Operation<ServerLevel> original) {
        ProtoSkySpawn forcedSpawn = ThreadLocals.forcedSpawn.get();
        if (forcedSpawn != null) {
            ResourceKey<Level> forcedWorld = forcedSpawn.spawnWorld();
            if (forcedWorld != null) {
                ServerLevel world = instance.getLevel(forcedWorld);
                if (world != null) {
                    return world;
                } else {
                    ProtoSkyMod.LOGGER.error("Unknown spawn world: {}", forcedWorld.location().toString());
                }
            }
        }
        return original.call(instance);
    }


    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;", shift = At.Shift.BEFORE))
    void setNewSpawnWorld(CallbackInfo ci,
                          @Local(name = "compoundTag") CompoundTag nbtCompound,
                          @Local(name = "resourceKey") LocalRef<ResourceKey<Level>> registryKey
    ) {
        if (nbtCompound == null) {
            ThreadLocals.forcedSpawn.set(ProtoSkyMod.spawnInfo);
            ResourceKey<Level> forcedWorld = ProtoSkyMod.spawnInfo.spawnWorld();
            if (forcedWorld != null)
                registryKey.set(forcedWorld);
        }
    }

    @Inject(method = "placeNewPlayer", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    void unsetNewSpawnWorld(CallbackInfo ci) {
        ThreadLocals.forcedSpawn.remove();
    }

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    ServerLevel setRespawnWorld(MinecraftServer instance, Operation<ServerLevel> original) {
        ThreadLocals.forcedSpawn.set(ProtoSkyMod.spawnInfo);
        ResourceKey<Level> forcedWorld = ProtoSkyMod.spawnInfo.spawnWorld();
        if (forcedWorld != null) {
            ServerLevel world = instance.getLevel(forcedWorld);
            if (world != null) {
                return world;
            } else {
                ProtoSkyMod.LOGGER.error("Unknown spawn world: {}", forcedWorld.location().toString());
            }
        }
        return original.call(instance);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    void resetForcedSpawn(CallbackInfoReturnable<ServerPlayer> cir) {
        ThreadLocals.forcedSpawn.remove();
    }
}
