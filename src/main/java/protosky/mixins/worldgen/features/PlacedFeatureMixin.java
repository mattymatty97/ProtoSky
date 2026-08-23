package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.ThreadLocals;
import protosky.interfaces.GenerationMaskHolder;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @Inject(method = "placeWithContext(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void trackFeature(CallbackInfoReturnable<Boolean> cir) {
        ThreadLocals.currentPlacedFeature.get().add((PlacedFeature) (Object) this);
    }

    @Inject(method = "method_39646(Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lorg/apache/commons/lang3/mutable/MutableBoolean;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"))
    private static void updateStructureMask(CallbackInfo ci,
                                            @Local(argsOnly = true) ConfiguredFeature<?, ?> configuredFeature,
                                            @Local(argsOnly = true) PlacementContext featurePlacementContext,
                                            @Local(argsOnly = true) BlockPos placedPos,
                                            @Share("names") LocalRef<ResourceKey<?>[]> names
    ) {
        WorldGenLevel level = featurePlacementContext.getLevel();

        if (!(level instanceof WorldGenRegion region)) return;

        Optional<ResourceKey<PlacedFeature>> placedFeatureKey = ProtoSkyMod.getPlacedFeatureRegistry(region)
                .getResourceKey(ThreadLocals.currentPlacedFeature.get().peek());
        Optional<ResourceKey<ConfiguredFeature<?, ?>>> configuredFeatureKey = ProtoSkyMod.getConfiguredFeatureRegistry(region)
                .getResourceKey(configuredFeature);
        Optional<ResourceKey<Feature<?>>> featureKey = ProtoSkyMod.getFeatureRegistry(region)
                .getResourceKey(configuredFeature.feature());
        ArrayList<ResourceKey<?>> list = new ArrayList<>(3);
        placedFeatureKey.ifPresent(list::add);
        configuredFeatureKey.ifPresent(list::add);
        featureKey.ifPresent(list::add);
        names.set(list.toArray(new ResourceKey[0]));

        if (!(region instanceof GenerationMaskHolder holder)) return;

        holder.protoSky$setMask(names.get(), placedPos);
    }

    @Inject(method = "method_39646", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/mutable/MutableBoolean;setTrue()V"))
    private static void vanillaCheckListener(CallbackInfo ci, @Share("wasGenerated") LocalBooleanRef wasGenerated) {
        wasGenerated.set(true);
    }

    @Inject(method = "method_39646", at = @At("RETURN"))
    private static void clearStructureMask(CallbackInfo ci,
                                           @Local(argsOnly = true) PlacementContext featurePlacementContext,
                                           @Local(argsOnly = true) BlockPos placedPos,
                                           @Share("names") LocalRef<ResourceKey<?>[]> names,
                                           @Share("wasGenerated") LocalBooleanRef wasGenerated
    ) {
        WorldGenLevel world = featurePlacementContext.getLevel();
        if (world instanceof GenerationMaskHolder holder) {
            holder.protoSky$logMask(wasGenerated.get());

            holder.protoSky$unsetMask(names.get(), placedPos);
        }
    }

    @Inject(method = "placeWithContext(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void forgetFeature(CallbackInfoReturnable<Boolean> cir) {
        ThreadLocals.currentPlacedFeature.get().poll();
    }
}
