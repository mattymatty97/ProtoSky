package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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

    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
    public void setThreadLocals(CallbackInfo ci, @Local(argsOnly = true) WorldGenLevel world) {
        ThreadLocals.currentRegion.set((WorldGenRegion) world);
    }


    @WrapOperation(method = "applyBiomeDecoration", at = @At(value = "NEW", target = "(J)Lnet/minecraft/world/level/levelgen/XoroshiroRandomSource;"))
    public XoroshiroRandomSource initGraceRandom(long seed, Operation<XoroshiroRandomSource> original,
                                                 @Share("graceRandom") LocalRef<WorldgenRandom> graceRandom
    ) {
        graceRandom.set(new WorldgenRandom(original.call(seed)));
        return original.call(seed);
    }

    @WrapOperation(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setDecorationSeed(JII)J"))
    public long initGraceRandom(WorldgenRandom instance, long worldSeed, int blockX, int blockZ, Operation<Long> original,
                                @Share("graceRandom") LocalRef<WorldgenRandom> graceRandom
    ) {
        original.call(graceRandom.get(), worldSeed, blockX, blockZ);
        return original.call(instance, worldSeed, blockX, blockZ);
    }

    @Inject(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;startsForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Ljava/util/List;"))
    private void trackStructure(CallbackInfo ci, @Local(name = "structure") Structure structure) {
        ThreadLocals.currentStructure.get().add(structure);
    }

    @Inject(method = "method_38265", at = @At(value = "HEAD"))
    public void updateStructureMask(CallbackInfo ci,
                                    @Local(argsOnly = true, name = "worldGenLevel") WorldGenLevel structureWorldAccess,
                                    @Local(argsOnly = true, name = "structureStart") StructureStart start,
                                    @Share("origin") LocalRef<BlockPos> origin,
                                    @Share("names") LocalRef<ResourceKey<?>[]> names
    ) {
        if (structureWorldAccess instanceof GenerationMaskHolder holder) {
            BoundingBox bbox = start.getPieces().get(0).getBoundingBox();
            BlockPos blockPos = bbox.getCenter();
            Optional<ResourceKey<Structure>> structureKey = ProtoSkyMod.getStructureRegistry(structureWorldAccess)
                    .getResourceKey(ThreadLocals.currentStructure.get().peek());
            ArrayList<ResourceKey<?>> list = new ArrayList<>();
            structureKey.ifPresent(list::add);
            names.set(list.toArray(new ResourceKey[0]));
            holder.protoSky$setMask(names.get(), blockPos);
            origin.set(blockPos);
        }
    }

    @Inject(method = "method_38265", at = @At(value = "RETURN"))
    public void clearStructureMask(CallbackInfo ci,
                                   @Local(argsOnly = true, name = "worldGenLevel") WorldGenLevel structureWorldAccess,
                                   @Share("origin") LocalRef<BlockPos> origin,
                                   @Share("names") LocalRef<ResourceKey<?>[]> names
    ) {
        if (structureWorldAccess instanceof GenerationMaskHolder holder) {

            holder.protoSky$logMask(true);

            holder.protoSky$unsetMask(names.get(), origin.get());
        }
    }

    @Inject(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void clearTrackedStructure(CallbackInfo ci) {
        ThreadLocals.currentStructure.get().poll();
    }

    @WrapOperation(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setFeatureSeed(JII)V"))
    public void setDecorationGraceRandom(WorldgenRandom instance, long populationSeed, int index, int step, Operation<Void> original,
                                         @Share("graceRandom") LocalRef<WorldgenRandom> graceRandom

    ) {
        original.call(graceRandom.get(), populationSeed, index, step);
        original.call(instance, populationSeed, index, step);
        ThreadLocals.graceRandom.set(graceRandom.get());
    }

    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    public void unsetGraceRandoms(CallbackInfo ci,
                                  @Local(argsOnly = true, name = "worldGenLevel") WorldGenLevel world) {
        ThreadLocals.graceRandom.remove();
        ThreadLocals.currentRegion.remove();
        if (world instanceof GenerationMaskHolder holder) {
            holder.protoSky$setMask(null, null);
        }
    }


}
