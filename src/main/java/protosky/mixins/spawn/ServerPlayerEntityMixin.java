package protosky.mixins.spawn;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import protosky.ProtoSkySpawn;
import protosky.ThreadLocals;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow @Final public MinecraftServer server;

    @ModifyReceiver(method = "moveToSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionType;hasSkyLight()Z"))
    private DimensionType fakeDimension(DimensionType instance){
        ProtoSkySpawn forcedSpawn = ThreadLocals.forcedSpawn.get();
        if (forcedSpawn != null && forcedSpawn.spawnWorld() != null)
            return this.server.getOverworld().getDimension();
        return instance;
    }

}
