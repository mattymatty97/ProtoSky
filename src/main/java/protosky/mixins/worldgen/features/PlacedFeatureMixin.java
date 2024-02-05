package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
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

    @Inject(method = "generate(Lnet/minecraft/world/gen/feature/FeaturePlacementContext;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"))
    private void trackFeature(FeaturePlacementContext context, Random random, BlockPos pos, CallbackInfoReturnable<Boolean> cir){
        ThreadLocals.currentPlacedFeature.get().add((PlacedFeature)(Object)this);
    }

    @Inject(method = "method_39646", at=@At("HEAD"))
    private static void updateStructureMask(ConfiguredFeature<?, ?> configuredFeature, FeaturePlacementContext featurePlacementContext, Random random, MutableBoolean mutableBoolean, BlockPos placedPos, CallbackInfo ci,
                                            @Share("names")LocalRef<RegistryKey<?>[]> names){
        ChunkRegion region = (ChunkRegion) featurePlacementContext.getWorld();
        Optional<RegistryKey<PlacedFeature>> placedFeatureKey = ProtoSkyMod.getPlacedFeatureRegistry(region)
                .getKey(ThreadLocals.currentPlacedFeature.get().peek());
        Optional<RegistryKey<ConfiguredFeature<?, ?>>> configuredFeatureKey = ProtoSkyMod.getConfiguredFeatureRegistry(region)
                .getKey(configuredFeature);
        Optional<RegistryKey<Feature<?>>> featureKey = ProtoSkyMod.getFeatureRegistry(region)
                .getKey(configuredFeature.feature());
        ArrayList<RegistryKey<?>> list = new ArrayList<>(3);
        placedFeatureKey.ifPresent(list::add);
        configuredFeatureKey.ifPresent(list::add);
        featureKey.ifPresent(list::add);
        names.set(list.toArray(new RegistryKey[0]));
        if (region instanceof GenerationMaskHolder holder){
            holder.protoSky$setMask(names.get(), placedPos);
        }
    }

    @Inject(method = "method_39646", at=@At(value = "INVOKE", target = "Lorg/apache/commons/lang3/mutable/MutableBoolean;setTrue()V"))
    private static void vanillaCheckListener(
            ConfiguredFeature<?,?> configuredFeature, FeaturePlacementContext featurePlacementContext, Random random, MutableBoolean mutableBoolean, BlockPos placedPos, CallbackInfo ci,
            @Share("wasGenerated")LocalBooleanRef wasGenerated){
        wasGenerated.set(true);
    }

    @Inject(method = "method_39646", at=@At("RETURN"))
    private static void clearStructureMask(ConfiguredFeature<?, ?> configuredFeature, FeaturePlacementContext featurePlacementContext, Random random, MutableBoolean mutableBoolean, BlockPos placedPos, CallbackInfo ci,
                                           @Share("names")LocalRef<RegistryKey<?>[]> names,
                                           @Share("wasGenerated")LocalBooleanRef wasGenerated){
        StructureWorldAccess world = featurePlacementContext.getWorld();
        if (world instanceof GenerationMaskHolder holder){
            holder.protoSky$logMask(wasGenerated.get());

            holder.protoSky$unsetMask(names.get(), placedPos);
        }
    }
    @Inject(method = "generate(Lnet/minecraft/world/gen/feature/FeaturePlacementContext;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/BlockPos;)Z", at = @At("RETURN"))
    private void forgetFeature(FeaturePlacementContext context, Random random, BlockPos pos, CallbackInfoReturnable<Boolean> cir){
        ThreadLocals.currentPlacedFeature.get().poll();
    }
}
