package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import protosky.ProtoSkyMod;
import protosky.ThreadLocals;
import protosky.interfaces.GenerationMaskHolder;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "generateFeatures", at = @At("HEAD"))
    public void setThreadLocals(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci){
        ThreadLocals.currentRegion.set((ChunkRegion)world);
    }


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

    @Inject(method = "generateFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/StructureAccessor;getStructureStarts(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/world/gen/structure/Structure;)Ljava/util/List;"))
    private void trackStructure(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci,
                                @Local Structure structure){
        ThreadLocals.currentStructure.get().add(structure);
    }

    @Inject(method = "method_38265", at = @At(value = "HEAD"))
    public void updateStructureMask(StructureWorldAccess structureWorldAccess, StructureAccessor structureAccessor, ChunkRandom chunkRandom, Chunk chunk, ChunkPos chunkPos, StructureStart start, CallbackInfo ci,
                                    @Share("origin") LocalRef<BlockPos> origin,
                                    @Share("names")LocalRef<RegistryKey<?>[]> names
    ){
        if (structureWorldAccess instanceof GenerationMaskHolder holder){
            BlockBox bbox = start.getChildren().get(0).getBoundingBox();
            BlockPos blockPos = bbox.getCenter();
            Optional<RegistryKey<Structure>> structureKey = ProtoSkyMod.getStructureRegistry(structureWorldAccess)
                    .getKey(ThreadLocals.currentStructure.get().peek());
            ArrayList<RegistryKey<?>> list = new ArrayList<>();
            structureKey.ifPresent(list::add);
            names.set(list.toArray(new RegistryKey[0]));
            holder.protoSky$setMask(names.get(), blockPos);
            origin.set(blockPos);
        }
    }

    @Inject(method = "method_38265", at = @At(value = "RETURN"))
    public void clearStructureMask(StructureWorldAccess structureWorldAccess, StructureAccessor structureAccessor, ChunkRandom chunkRandom, Chunk chunk, ChunkPos chunkPos, StructureStart start, CallbackInfo ci,
                                   @Share("origin") LocalRef<BlockPos> origin,
                                   @Share("names")LocalRef<RegistryKey<?>[]> names
    ){
        if (structureWorldAccess instanceof GenerationMaskHolder holder){

            holder.protoSky$logMask(true);

            holder.protoSky$unsetMask(names.get(), origin.get());
        }
    }

    @Inject(method = "generateFeatures", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void clearTrackedStructure(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci){
        ThreadLocals.currentStructure.get().poll();
    }

    @WrapOperation(method = "generateFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/ChunkRandom;setDecoratorSeed(JII)V"))
    public void setDecorationGraceRandom(ChunkRandom instance, long populationSeed, int index, int step, Operation<Void> original,
                                        @Share("graceRandom") LocalRef<ChunkRandom> graceRandom

    ){
        original.call(graceRandom.get(),populationSeed,index, step);
        original.call(instance,populationSeed,index, step);
        ThreadLocals.graceRandom.set(graceRandom.get());
    }

    @Inject(method = "generateFeatures", at =@At("RETURN"))
    public void unsetGraceRandoms(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci){
        ThreadLocals.graceRandom.remove();
        ThreadLocals.currentRegion.remove();
        if (world instanceof GenerationMaskHolder holder){
            holder.protoSky$setMask(null,null);
        }
    }


}
