package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.feature.GeodeFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import protosky.Debug;
import protosky.ProtoSkyMod;
import protosky.interfaces.GenerationMaskHolder;
import protosky.mixins.accessors.ChunkRegionAccessor;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {

    @Inject(method = "method_39646", at=@At("HEAD"))
    private static void updateStructureMask(ConfiguredFeature<?, ?> configuredFeature, FeaturePlacementContext featurePlacementContext, Random random, MutableBoolean mutableBoolean, BlockPos placedPos, CallbackInfo ci,
                                            @Share("shouldReset")LocalBooleanRef reset){
        if (featurePlacementContext.getWorld() instanceof GenerationMaskHolder holder && holder.protoSky$getMask() == null){
            ChunkRegion region = (ChunkRegion) holder;
            String structureName = ((ChunkRegionAccessor)region).getCurrentlyGeneratingStructureName().get();
            holder.protoSky$updateMask(structureName, placedPos);
            reset.set(true);
        }
    }

    @Inject(method = "method_39646", at=@At("RETURN"))
    private static void clearStructureMask(ConfiguredFeature<?, ?> configuredFeature, FeaturePlacementContext featurePlacementContext, Random random, MutableBoolean mutableBoolean, BlockPos placedPos, CallbackInfo ci,
                                           @Share("shouldReset")LocalBooleanRef reset){
        if (featurePlacementContext.getWorld() instanceof GenerationMaskHolder holder && reset.get()){
            ChunkRegion region = (ChunkRegion) holder;
            String structureName = ((ChunkRegionAccessor)region).getCurrentlyGeneratingStructureName().get();
            holder.protoSky$updateMask(structureName, null);
        }
    }

    /*
    //TODO remove debug methods
    @WrapOperation(method = "method_39646", at=@At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/ConfiguredFeature;generate(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean debugGeodes(ConfiguredFeature<?, ?> instance, StructureWorldAccess world, ChunkGenerator chunkGenerator, Random random, BlockPos origin, Operation<Boolean> original){
        boolean flag = original.call(instance, world, chunkGenerator, random, origin);
        boolean flag2 = (world instanceof GenerationMaskHolder holder && holder.protoSky$getMask() != ProtoSkyMod.EMPTY_MASK);
        if (instance.feature() instanceof GeodeFeature){
            long total = Debug.totalGeodes.incrementAndGet();
            long vanilla = Debug.vanillaGeodes.addAndGet(flag?1:0);
            long graced = Debug.gracedGeodes.addAndGet(flag2?1:0);
            long generated = Debug.generatedGeodes.addAndGet((flag && flag2)?1:0);
            ChunkPos center = ((ChunkRegion)world).getCenterPos();
            ProtoSkyMod.LOGGER.warn(
                    "ChunkRegion ({}{}) attempted to generate geode at [{},{},{}]| wasGenerated={}, wasGraced={}, total={}, graced={}, vanilla={}, generated={}",
                    center.x, center.z,
                    origin.getX(), origin.getY(), origin.getZ(),
                    flag,
                    flag2,
                    total,
                    graced,
                    vanilla,
                    generated
            );
        }
        return flag;
    }
    */
}
