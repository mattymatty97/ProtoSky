package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.interfaces.FeatureWorldMask;
import protosky.interfaces.GraceHolder;
import protosky.interfaces.GenerationMaskHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin implements GenerationMaskHolder {
    @Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth);

    @Shadow public abstract long getSeed();

    @Shadow public abstract ChunkManager getChunkManager();

    @Shadow public abstract @Nullable Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);

    @Unique
    private FeatureWorldMask mask = null;

    @Unique
    private final Map<ChunkPos,Set<BlockPos>> blocks_to_rollback = new ConcurrentHashMap<>();

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkRegion;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;", shift = At.Shift.BEFORE))
    private void checkSetBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalRef<BlockState> forced_state){
        if (this.mask!=null) {
            Chunk chunk = ((WorldView) this).getChunk(pos);
            Random random = ProtoSkyMod.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while placing block {} ({},{},{})", state.toString(), pos.getX(), pos.getY(), pos.getZ());
                random = Random.createLocal();
            }
            if (this.mask.canPlace(forced_state, random.nextDouble())) {
                ((GraceHolder) chunk).protoSky$putGracedBlock(pos, forced_state.get());
                blocks_to_rollback.computeIfAbsent(chunk.getPos(), k->new HashSet<>()).remove(pos.toImmutable());
                return;
            } else {
                if (((GraceHolder) chunk).protoSky$getGracedBlocks().get(pos) != null)
                    ((GraceHolder) chunk).protoSky$putGracedBlock(pos, null);
            }
            blocks_to_rollback.computeIfAbsent(chunk.getPos(), k->new HashSet<>()).add(pos.toImmutable());
        }
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void checkSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalRef<Entity> forced_entity){
        if (this.mask != null) {
            Chunk chunk = ((WorldView) this).getChunk(entity.getBlockPos());
            Random random = ProtoSkyMod.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while spawning entity {} ({},{},{})", entity.toString(), entity.getX(), entity.getY(), entity.getZ());
                random = Random.createLocal();
            }
            if (this.mask.canSpawn(forced_entity, random.nextDouble())) {
                ((GraceHolder) chunk).protoSky$putGracedEntity(forced_entity.get());
                return;
            }

            cir.setReturnValue(true);
        }
    }

    @Override
    public FeatureWorldMask protoSky$getMask() {
        return this.mask;
    }

    @Override
    public void protoSky$updateMask(String name, BlockPos origin) {
        if (name != null && origin!=null) {
            ChunkRandom structureRandom = new ChunkRandom(new CheckedRandom(0L));
            structureRandom.setCarverSeed(this.getSeed(), origin.getX(), origin.getZ());
            FeatureWorldMask mask = ProtoSkyMod.baked_masks.getOrDefault(name, ProtoSkyMod.EMPTY_MASK);
            boolean flag = mask.hasGraces(structureRandom.nextDouble());

            if (flag)
                this.mask = (mask);
            else
                this.mask = (ProtoSkyMod.EMPTY_MASK);

        }else {
            this.mask = null;
        }

        if (name == null && origin == null){
            blocks_to_rollback.entrySet().stream()
                    //rollback only blocks in already completed chunks
                    .filter((e) -> this.getChunk(e.getKey().x, e.getKey().z, ChunkStatus.INITIALIZE_LIGHT,false) != null)
                    .flatMap(e -> e.getValue().stream())
                    .forEach((blockPos) -> {
                        this.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 0 ,0);
            });
            blocks_to_rollback.values().forEach(Set::clear);
            blocks_to_rollback.clear();
        }
    }
}
