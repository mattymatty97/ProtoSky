package protosky.mixins.worldgen;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public class EnderDragonFightMixin {
    @Shadow
    private BlockPos exitPortalLocation;

    @Shadow
    @Final
    private ServerWorld world;

    @Inject(method = "generateEndPortal(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/EndPortalFeature;generateIfValid(Lnet/minecraft/world/gen/feature/FeatureConfig;Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/BlockPos;)Z", shift = At.Shift.BEFORE))
    private void adjustExitPortalLocation(boolean open, CallbackInfo ci) {
        int portalHeight = exitPortalLocation.getY();

        if (portalHeight < 1) {
            portalHeight = this.world.getChunk(exitPortalLocation).sampleHeightmap(Heightmap.Type.PROTO_SKY_VANILLA_WORLD_SURFACE, exitPortalLocation.getX(), exitPortalLocation.getZ());

            exitPortalLocation = new BlockPos(exitPortalLocation.getX(), Math.max(portalHeight, 1), exitPortalLocation.getZ());
        }
    }
}
