package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.Debug;
import protosky.ProtoSkyMod;
import protosky.ThreadLocals;
import protosky.interfaces.FeatureWorldMask;
import protosky.interfaces.GraceHolder;
import protosky.interfaces.GenerationMaskHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin implements GenerationMaskHolder {
    @Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth);

    @Shadow public abstract long getSeed();

    @Shadow public abstract ChunkPos getCenterPos();

    @Shadow public abstract ServerWorld toServerWorld();

    @Unique
    private FeatureWorldMask mask;
    @Unique
    private String[] maskReference;
    @Unique
    private BlockPos maskOrigin;

    @Unique
    private Set<BlockPos> blocks_to_rollback;

    @Inject(method = "<init>", at =@At("RETURN"))
    private void init(ServerWorld world, List<?> chunks, ChunkStatus status, int placementRadius, CallbackInfo ci){
        mask = null;
        maskReference = null;
        blocks_to_rollback = new HashSet<>();
    }

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkRegion;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;", shift = At.Shift.BEFORE))
    private void checkSetBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir,
                               @Local(argsOnly = true) LocalRef<BlockState> forced_state){
        if (this.mask!=null) {
            Chunk chunk = ((WorldView) this).getChunk(pos);
            Random random = ThreadLocals.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while placing block {} ({},{},{})", state.toString(), pos.getX(), pos.getY(), pos.getZ());
                random = Random.createLocal();
            }
            if (this.mask.canPlace(forced_state, random.nextDouble())) {
                ((GraceHolder) chunk).protoSky$putGracedBlock(pos, forced_state.get());
                this.protoSky$updateRollbacks(pos.toImmutable(), true);
                return;
            } else {
                if (((GraceHolder) chunk).protoSky$getGracedBlocks().get(pos) != null)
                    ((GraceHolder) chunk).protoSky$putGracedBlock(pos, null);
            }
            this.protoSky$updateRollbacks(pos.toImmutable(),false);
        }
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void checkSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir,
                                  @Local(argsOnly = true) LocalRef<Entity> forced_entity){
        if (this.mask != null) {
            Chunk chunk = ((WorldView) this).getChunk(entity.getBlockPos());
            Random random = ThreadLocals.graceRandom.get();
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
    public void protoSky$updateRollbacks(BlockPos pos, boolean remove) {
        if (remove){
            blocks_to_rollback.remove(pos.toImmutable());
        }else {
            blocks_to_rollback.add(pos.toImmutable());
        }
    }

    @Override
    public void protoSky$updateMask(String[] names, BlockPos origin) {
        if (names != null && names.length > 0 && origin!=null && !ProtoSkyMod.ignoredWorlds.contains(this.toServerWorld().getRegistryKey())) {
            ChunkRandom structureRandom = new ChunkRandom(new CheckedRandom(0L));
            structureRandom.setCarverSeed(this.getSeed(), origin.getX(), origin.getZ());
            FeatureWorldMask mask = ProtoSkyMod.EMPTY_MASK;

            for (String name : names){
                mask = ProtoSkyMod.baked_masks.getOrDefault(name, ProtoSkyMod.EMPTY_MASK);
                if (mask !=  ProtoSkyMod.EMPTY_MASK) {
                    break;
                }
            }

            boolean flag = mask.hasGraces(structureRandom.nextDouble());

            if (flag)
                this.mask = (mask);
            else
                this.mask = (ProtoSkyMod.EMPTY_MASK);

            this.maskReference = names;
            this.maskOrigin = origin.toImmutable();
        }else {
            this.mask = null;
            this.maskReference = null;
            this.maskOrigin= null;
        }

        if (names == null && origin == null){
            blocks_to_rollback.forEach((blockPos) -> {
                        this.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 0 ,0);
            });
            blocks_to_rollback.clear();
        }
    }

    @Override
    public void protoSky$logMask(String[] names, BlockPos origin, boolean wasGenerated) {
        if (this.mask != null) {
            
            Debug.AttemptCounter counter = null;
            if (Debug.anyAttempt)
                counter = Debug.attemptMap.computeIfAbsent(this.maskReference[0], (i) -> new Debug.AttemptCounter());
            else if (!Debug.attemptMap.isEmpty()){
                for (String name : this.maskReference) {
                    counter = Debug.attemptMap.get(name);
                    if (counter != null)
                        break;
                }
            }

            if (counter != null) {
                boolean isSubset = !Arrays.equals(names, this.maskReference);
                boolean wasGraced = this.mask != ProtoSkyMod.EMPTY_MASK;

                counter.total().add(this.maskOrigin);
                if (wasGraced)
                    counter.graced().add(this.maskOrigin);
                if (wasGenerated)
                    counter.vanilla().add(this.maskOrigin);
                if (wasGraced && wasGenerated)
                    counter.generated().add(this.maskOrigin);

                ChunkPos center = this.getCenterPos();
                if (!isSubset) {
                    ProtoSkyMod.LOGGER.warn(
                            "ChunkRegion ({} {}) attempted to generate {} at [{} {} {}]| wasGenerated={}, wasGraced={}| total={}, graced={}, vanilla={}, generated={}",
                            center.x, center.z,
                            Arrays.toString(names),
                            origin.getX(), origin.getY(), origin.getZ(),
                            wasGenerated,
                            wasGraced,
                            counter.total().size(),
                            counter.graced().size(),
                            counter.vanilla().size(),
                            counter.generated().size()
                    );
                }else{
                    ProtoSkyMod.LOGGER.warn(
                            "ChunkRegion ({} {}) attempted to generate {} at [{} {} {}] as part of {} at [{} {} {}]| wasGenerated={}, wasGraced={}| total={}, graced={}, vanilla={}, generated={}",
                            center.x, center.z,
                            Arrays.toString(names),
                            origin.getX(), origin.getY(), origin.getZ(),
                            Arrays.toString(this.maskReference),
                            this.maskOrigin.getX(), this.maskOrigin.getY(), this.maskOrigin.getZ(),
                            wasGenerated,
                            wasGraced,
                            counter.total().size(),
                            counter.graced().size(),
                            counter.vanilla().size(),
                            counter.generated().size()
                    );
                }
            }
        }

    }
}
