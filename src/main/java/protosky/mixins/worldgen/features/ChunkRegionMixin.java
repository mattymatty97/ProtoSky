package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
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
import org.spongepowered.asm.mixin.Final;
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

import java.util.*;
import java.util.stream.Collectors;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin implements GenerationMaskHolder {
    @Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth);

    @Shadow public abstract long getSeed();

    @Shadow public abstract ChunkPos getCenterPos();

    @Shadow public abstract ServerWorld toServerWorld();

    @Unique
    private LinkedList<FeatureWorldMask> masks;
    @Unique
    private LinkedList<RegistryKey<?>[]> maskReferences;
    @Unique
    private LinkedList<BlockPos> maskOrigins;

    @Unique
    private Set<BlockPos> blocks_to_rollback;

    @Inject(method = "<init>", at =@At("RETURN"))
    private void init(ServerWorld world, List<?> chunks, ChunkStatus status, int placementRadius, CallbackInfo ci){
        masks = new LinkedList<>();
        maskReferences = new LinkedList<>();
        maskOrigins = new LinkedList<>();
        blocks_to_rollback = new HashSet<>();
    }

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkRegion;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;", shift = At.Shift.BEFORE))
    private void checkSetBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir,
                               @Local(argsOnly = true) LocalRef<BlockState> forced_state){
        FeatureWorldMask mask = this.protoSky$getMask();
        if (mask != null) {
            Chunk chunk = ((WorldView) this).getChunk(pos);
            Random random = ThreadLocals.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while placing block {} ({},{},{})", state.toString(), pos.getX(), pos.getY(), pos.getZ());
                random = Random.createLocal();
            }
            if (mask.canPlace(forced_state, random.nextDouble())) {
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
        FeatureWorldMask mask = this.protoSky$getMask();
        if (mask != null) {
            Chunk chunk = ((WorldView) this).getChunk(entity.getBlockPos());
            Random random = ThreadLocals.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while spawning entity {} ({},{},{})", entity.toString(), entity.getX(), entity.getY(), entity.getZ());
                random = Random.createLocal();
            }
            if (mask.canSpawn(forced_entity, random.nextDouble())) {
                ((GraceHolder) chunk).protoSky$putGracedEntity(forced_entity.get());
                return;
            }

            cir.setReturnValue(true);
        }
    }

    @Override
    public FeatureWorldMask protoSky$getMask() {
        return this.masks.peekLast();
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
    public void protoSky$setMask(RegistryKey<?>[] keys, BlockPos origin) {
        if (keys != null && keys.length > 0 && origin!=null &&
                !ProtoSkyMod.ignoredWorlds.contains(this.toServerWorld().getRegistryKey())) {
            FeatureWorldMask currMask = this.masks.peek();
            if (currMask == null || currMask.isReplaceable()) {

                ChunkRandom structureRandom = new ChunkRandom(new CheckedRandom(0L));
                structureRandom.setCarverSeed(this.getSeed(), origin.getX(), origin.getZ());
                FeatureWorldMask foundMask = ProtoSkyMod.DEFAULT_MASK;

                {
                    FeatureWorldMask tmpMask;
                    for (RegistryKey<?> name : keys) {
                        tmpMask = ProtoSkyMod.baked_masks.getOrDefault(name, ProtoSkyMod.DEFAULT_MASK);
                        if (tmpMask != ProtoSkyMod.DEFAULT_MASK && foundMask.isReplaceable()) {
                            foundMask = tmpMask;
                        }
                    }
                }

                double random = structureRandom.nextDouble();

                boolean flag = ( ( foundMask == ProtoSkyMod.DEFAULT_MASK ) || foundMask.canGenerate(random) );

                if (flag)
                    this.masks.addLast(foundMask);
                else
                    this.masks.addLast(ProtoSkyMod.EMPTY_MASK);
            }else{
                this.masks.addLast(currMask);
            }

            this.maskOrigins.addLast(origin.toImmutable());
            this.maskReferences.addLast(keys);
        }else {
            this.masks.clear();
            this.maskReferences.clear();
            this.maskOrigins.clear();
            ChunkPos chunkPos = this.getCenterPos();
            for (BlockPos blockPos : blocks_to_rollback) {
                this.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 0, 0);
            }
            blocks_to_rollback.clear();
        }
    }

    @Override
    public void protoSky$unsetMask(RegistryKey<?>[] keys, BlockPos origin) {
        if (keys != null && keys.length > 0 && origin!=null &&
                !ProtoSkyMod.ignoredWorlds.contains(this.toServerWorld().getRegistryKey())) {
            this.maskOrigins.pollLast();
            this.maskReferences.pollLast();
            this.masks.pollLast();
        }
    }

    @Override
    public void protoSky$logMask(boolean wasGenerated) {
        try {
            FeatureWorldMask mask = this.protoSky$getMask();
            if (mask != null) {

                Debug.AttemptCounter counter = null;
                RegistryKey<?> logKey = null;
                int index = -1;
                List<RegistryKey<?>> fullReference = this.maskReferences.stream().flatMap(Arrays::stream).toList();
                if (Debug.anyAttempt) {
                    assert this.maskReferences.peekFirst() != null;
                    logKey = this.maskReferences.peekFirst()[0];
                    counter = Debug.attemptMap.computeIfAbsent(logKey.toString(), (i) -> new Debug.AttemptCounter());
                    index = 0;
                } else if (!Debug.attemptMap.isEmpty()) {
                    for (RegistryKey<?>[] keys : this.maskReferences) {
                        index++;
                        for (RegistryKey<?> key : keys) {
                            counter = Debug.attemptMap.get(key.toString());
                            if (counter != null) {
                                logKey = key;
                                break;
                            }
                        }

                        if(logKey != null)
                            break;
                    }
                }

                if (counter != null) {
                    String referenceString = fullReference.stream()
                            .map(RegistryKey::toString)
                            .collect(Collectors.joining(" -> ", "\"", "\""));
                    boolean isSubset = this.maskReferences.size() > 1;
                    boolean wasGraced = ( ( mask != ProtoSkyMod.EMPTY_MASK ) && ( mask != ProtoSkyMod.DEFAULT_MASK ) );

                    BlockPos logOrigin = this.maskOrigins.get(index);
                    assert logOrigin != null;

                    counter.total().add(logOrigin);
                    if (wasGraced)
                        counter.graced().add(logOrigin);
                    if (wasGenerated)
                        counter.vanilla().add(logOrigin);
                    if (wasGraced && wasGenerated)
                        counter.generated().add(logOrigin);

                    ChunkPos center = this.getCenterPos();
                    if (!isSubset) {
                        ProtoSkyMod.LOGGER.warn(
                                "ChunkRegion ({} {}) attempted to generate {} at [{} {} {}]| wasGenerated={}, wasGraced={}| total={}, graced={}, vanilla={}, generated={}",
                                center.x, center.z,
                                referenceString,
                                logOrigin.getX(), logOrigin.getY(), logOrigin.getZ(),
                                wasGenerated,
                                wasGraced,
                                counter.total().size(),
                                counter.graced().size(),
                                counter.vanilla().size(),
                                counter.generated().size()
                        );
                    } else {
                        BlockPos subOrigin = this.maskOrigins.peekLast();
                        assert subOrigin != null;
                        ProtoSkyMod.LOGGER.warn(
                                "ChunkRegion ({} {}) attempted to generate {} at [{} {} {}] with parent at [{} {} {}]| wasGenerated={}, wasGraced={}| total={}, graced={}, vanilla={}, generated={}",
                                center.x, center.z,
                                referenceString,
                                subOrigin.getX(), subOrigin.getY(), subOrigin.getZ(),
                                logOrigin.getX(), logOrigin.getY(), logOrigin.getZ(),
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
        }catch (Throwable t){
            ProtoSkyMod.LOGGER.error("Error while logging", t);
        }
    }
}
