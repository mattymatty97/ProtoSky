package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ThreadLocals;
import protosky.datapack.ProtoSkySpawn;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Shadow
    @Final
    public MinecraftServer server;

    @ModifyReceiver(method = "fudgeSpawnLocation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;hasSkyLight()Z"))
    private DimensionType fakeDimension(DimensionType instance) {
        ProtoSkySpawn forcedSpawn = ThreadLocals.forcedSpawn.get();
        if (forcedSpawn != null && forcedSpawn.spawnWorld() != null)
            return this.server.overworld().dimensionType();
        return instance;
    }

}
