package protosky.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndDragonFight.class)
public class EndDragonFightMixin {
    @Shadow
    private BlockPos portalLocation;

    @Inject(method = "spawnExitPortal(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/EndPodiumFeature;place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z", shift = At.Shift.BEFORE))
    private void adjustExitPortalLocation(CallbackInfo ci) {
        if (portalLocation.getY() < 1)
            portalLocation = new BlockPos(portalLocation.getX(), 1, portalLocation.getZ());
    }
}
