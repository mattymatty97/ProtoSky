package protosky.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndDragonFight.class)
public class EndDragonFightMixin {
    @Shadow
    private BlockPos portalLocation;

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "spawnExitPortal(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/EndPodiumFeature;place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z", shift = At.Shift.BEFORE))
    private void adjustExitPortalLocation(boolean open, CallbackInfo ci) {
        int portalHeight = portalLocation.getY();

        if (portalHeight < 1)
        {
            portalHeight = this.level.getChunk(portalLocation).getHeight(Heightmap.Types.PROTO_SKY_VANILLA_WORLD_SURFACE, portalLocation.getX(), portalLocation.getZ());

            portalLocation = new BlockPos(portalLocation.getX(), Math.max(portalHeight, 1), portalLocation.getZ());
        }
    }
}
