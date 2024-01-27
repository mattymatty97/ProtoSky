package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.registry.RegistryKeys;
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
import protosky.interfaces.GenerationMaskHolder;
import protosky.mixins.accessors.ChunkRegionAccessor;

import java.util.Objects;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @Inject(method = "method_39646", at=@At("HEAD"))
    private static void updateStructureMask(ConfiguredFeature<?, ?> configuredFeature, FeaturePlacementContext featurePlacementContext, Random random, MutableBoolean mutableBoolean, BlockPos placedPos, CallbackInfo ci,
                                            @Share("shouldReset")LocalBooleanRef reset,
                                            @Share("names")LocalRef<String[]> names){
        ChunkRegion region = (ChunkRegion) featurePlacementContext.getWorld();
        String placedFeatureName = ((ChunkRegionAccessor)region).getCurrentlyGeneratingStructureName().get();
        String configuredFeatureName = region.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE)
                .getKey(configuredFeature).map(Object::toString).orElse(null);
        String featureName = region.getRegistryManager().get(RegistryKeys.FEATURE)
                .getKey(configuredFeature.feature())
                .map(Objects::toString)
                .orElse("");
        names.set(new String[]{placedFeatureName, configuredFeatureName, featureName});
        if (region instanceof GenerationMaskHolder holder && holder.protoSky$getMask() == null){

            holder.protoSky$updateMask(names.get(), placedPos);
            reset.set(true);
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
                                           @Share("shouldReset")LocalBooleanRef reset,
                                           @Share("names")LocalRef<String[]> names,
                                           @Share("wasGenerated")LocalBooleanRef wasGenerated){
        StructureWorldAccess world = featurePlacementContext.getWorld();
        if (world instanceof GenerationMaskHolder holder){
            holder.protoSky$logMask(names.get(), placedPos, wasGenerated.get());

            if (reset.get())
                holder.protoSky$updateMask(names.get(), null);
        }
    }
}
