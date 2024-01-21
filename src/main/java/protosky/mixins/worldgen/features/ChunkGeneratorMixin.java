package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import protosky.ProtoSkyMod;
import protosky.interfaces.GenerationMaskHolder;
import protosky.mixins.accessors.ChunkRegionAccessor;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @WrapOperation(method = "generateFeatures", at = @At(value = "NEW", target = "(J)Lnet/minecraft/util/math/random/Xoroshiro128PlusPlusRandom;"))
    public Xoroshiro128PlusPlusRandom initGraceRandom(long seed, Operation<Xoroshiro128PlusPlusRandom> original,
                                                      @Share("graceRandom") LocalRef<ChunkRandom> graceRandom
    ){
        graceRandom.set(new ChunkRandom(original.call(seed)));
        return original.call(seed);
    }

    @WrapOperation(method = "generateFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/ChunkRandom;setPopulationSeed(JII)J"))
    public long initGraceRandom(ChunkRandom instance, long worldSeed, int blockX, int blockZ, Operation<Long> original,
                                @Share("graceRandom") LocalRef<ChunkRandom> graceRandom
    ){
        original.call(graceRandom.get(), worldSeed, blockX, blockZ);
        return original.call(instance, worldSeed, blockX, blockZ);
    }

    @Inject(method = "method_38265", at = @At(value = "HEAD"))
    public void updateStructureMask(StructureWorldAccess structureWorldAccess, StructureAccessor structureAccessor, ChunkRandom chunkRandom, Chunk chunk, ChunkPos chunkPos, StructureStart start, CallbackInfo ci,
                                    @Share("shouldReset") LocalBooleanRef reset
    ){
        if (structureWorldAccess instanceof GenerationMaskHolder holder && holder.protoSky$getMask() == null){
            BlockBox bbox = start.getChildren().get(0).getBoundingBox();
            BlockPos blockPos = bbox.getCenter();
            ChunkRegion region = (ChunkRegion) structureWorldAccess;
            String structureName = ((ChunkRegionAccessor)region).getCurrentlyGeneratingStructureName().get();
            holder.protoSky$updateMask(structureName, blockPos);
            reset.set(true);
        }
    }

    @Inject(method = "method_38265", at = @At(value = "RETURN"))
    public void clearStructureMask(StructureWorldAccess structureWorldAccess, StructureAccessor structureAccessor, ChunkRandom chunkRandom, Chunk chunk, ChunkPos chunkPos, StructureStart start, CallbackInfo ci,
                                   @Share("shouldReset")LocalBooleanRef reset
    ){
        if (structureWorldAccess instanceof GenerationMaskHolder holder && reset.get()){
            ChunkRegion region = (ChunkRegion) structureWorldAccess;
            String structureName = ((ChunkRegionAccessor)region).getCurrentlyGeneratingStructureName().get();
            holder.protoSky$updateMask(structureName, null);
        }
    }

    @WrapOperation(method = "generateFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/ChunkRandom;setDecoratorSeed(JII)V"))
    public void setDecorationGraceRandom(ChunkRandom instance, long populationSeed, int index, int step, Operation<Void> original,
                                        @Share("graceRandom") LocalRef<ChunkRandom> graceRandom

    ){
        original.call(graceRandom.get(),populationSeed,index, step);
        original.call(instance,populationSeed,index, step);
        ProtoSkyMod.graceRandom.set(graceRandom.get());
    }

    @Inject(method = "generateFeatures", at =@At("RETURN"))
    public void unsetGraceRandoms(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci){
        ProtoSkyMod.graceRandom.remove();
        if (world instanceof GenerationMaskHolder holder){
            holder.protoSky$updateMask(null,null);
        }
    }


}
