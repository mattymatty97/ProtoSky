package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
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
import protosky.interfaces.GenerationMaskHolder;
import protosky.interfaces.GraceHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements GenerationMaskHolder {
    @Shadow
    public abstract boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth);

    @Shadow
    public abstract long getSeed();

    @Shadow
    public abstract ChunkPos getCenter();

    @Shadow
    public abstract ServerLevel getLevel();

    @Unique
    private LinkedList<FeatureWorldMask> masks;
    @Unique
    private LinkedList<ResourceKey<?>[]> maskReferences;
    @Unique
    private LinkedList<BlockPos> maskOrigins;

    @Unique
    private Set<BlockPos> blocks_to_rollback;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        masks = new LinkedList<>();
        maskReferences = new LinkedList<>();
        maskOrigins = new LinkedList<>();
        blocks_to_rollback = new HashSet<>();
    }

    @Inject(method = "setBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;getChunk(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/ChunkAccess;", shift = At.Shift.BEFORE))
    private void checkSetBlock(CallbackInfoReturnable<Boolean> cir,
                               @Local(argsOnly = true, name = "blockPos") BlockPos pos,
                               @Local(argsOnly = true, name = "blockState") BlockState state,
                               @Local(argsOnly = true, name = "blockState") LocalRef<BlockState> forced_state
    ) {
        FeatureWorldMask mask = this.protoSky$getMask();
        if (mask != null) {
            ChunkAccess chunk = ((LevelReader) this).getChunk(pos);
            RandomSource random = ThreadLocals.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while placing block {} ({},{},{})", state.toString(), pos.getX(), pos.getY(), pos.getZ());
                random = RandomSource.createNewThreadLocalInstance();
            }
            if (mask.canPlace(forced_state, random.nextDouble())) {
                ((GraceHolder) chunk).protoSky$putGracedBlock(pos, forced_state.get());
                this.protoSky$updateRollbacks(pos.immutable(), true);
                return;
            } else {
                if (((GraceHolder) chunk).protoSky$getGracedBlocks().get(pos) != null)
                    ((GraceHolder) chunk).protoSky$putGracedBlock(pos, null);
            }
            this.protoSky$updateRollbacks(pos.immutable(), false);
        }
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void checkSpawnEntity(CallbackInfoReturnable<Boolean> cir,
                                  @Local(argsOnly = true, name = "entity") Entity entity,
                                  @Local(argsOnly = true, name = "entity") LocalRef<Entity> forced_entity) {
        FeatureWorldMask mask = this.protoSky$getMask();
        if (mask != null) {
            ChunkAccess chunk = ((LevelReader) this).getChunk(entity.blockPosition());
            RandomSource random = ThreadLocals.graceRandom.get();
            if (random == null) {
                ProtoSkyMod.LOGGER.warn("Missing random while spawning entity {} ({},{},{})", entity.toString(), entity.getX(), entity.getY(), entity.getZ());
                random = RandomSource.createNewThreadLocalInstance();
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
        if (remove) {
            blocks_to_rollback.remove(pos.immutable());
        } else {
            blocks_to_rollback.add(pos.immutable());
        }
    }

    @Override
    public void protoSky$setMask(ResourceKey<?>[] keys, BlockPos origin) {
        if (keys != null && keys.length > 0 && origin != null &&
                !ProtoSkyMod.ignoredWorlds.contains(this.getLevel().dimension())) {
            FeatureWorldMask currMask = this.masks.peek();
            if (currMask == null || currMask.isReplaceable()) {

                WorldgenRandom structureRandom = new WorldgenRandom(new LegacyRandomSource(0L));
                structureRandom.setLargeFeatureSeed(this.getSeed(), origin.getX(), origin.getZ());
                FeatureWorldMask foundMask = ProtoSkyMod.DEFAULT_MASK;

                {
                    FeatureWorldMask tmpMask;
                    for (ResourceKey<?> name : keys) {
                        tmpMask = ProtoSkyMod.baked_masks.getOrDefault(name, ProtoSkyMod.DEFAULT_MASK);
                        if (tmpMask != ProtoSkyMod.DEFAULT_MASK && foundMask.isReplaceable()) {
                            foundMask = tmpMask;
                        }
                    }
                }

                double random = structureRandom.nextDouble();

                boolean flag = ((foundMask == ProtoSkyMod.DEFAULT_MASK) || foundMask.canGenerate(random));

                if (flag)
                    this.masks.addLast(foundMask);
                else
                    this.masks.addLast(ProtoSkyMod.EMPTY_MASK);
            } else {
                this.masks.addLast(currMask);
            }

            this.maskOrigins.addLast(origin.immutable());
            this.maskReferences.addLast(keys);
        } else {
            this.masks.clear();
            this.maskReferences.clear();
            this.maskOrigins.clear();
            ChunkPos chunkPos = this.getCenter();
            for (BlockPos blockPos : blocks_to_rollback) {
                if (!chunkPos.equals(new ChunkPos(blockPos)))
                    this.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 0, 0);
            }
            blocks_to_rollback.clear();
        }
    }

    @Override
    public void protoSky$unsetMask(ResourceKey<?>[] keys, BlockPos origin) {
        if (keys != null && keys.length > 0 && origin != null &&
                !ProtoSkyMod.ignoredWorlds.contains(this.getLevel().dimension())) {
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
                ResourceKey<?> logKey = null;
                int index = -1;

                if (Debug.anyAttempt) {
                    assert this.maskReferences.peekFirst() != null;
                    logKey = this.maskReferences.peekFirst()[0];
                    counter = Debug.attemptMap.computeIfAbsent(logKey.toString(), (i) -> new Debug.AttemptCounter());
                    index = 0;
                } else if (!Debug.attemptMap.isEmpty()) {
                    for (ResourceKey<?>[] keys : this.maskReferences) {
                        index++;
                        for (ResourceKey<?> key : keys) {
                            counter = Debug.attemptMap.get(key.toString());
                            if (counter != null) {
                                logKey = key;
                                break;
                            }
                        }

                        if (logKey != null)
                            break;
                    }
                }

                if (counter != null) {
                    String referenceString = this.maskReferences.stream()
                            .map(Arrays::toString)
                            .collect(Collectors.joining(" -> ", "\"", "\""));
                    boolean isSubset = this.maskReferences.size() > 1;
                    boolean wasGraced = ((mask != ProtoSkyMod.EMPTY_MASK) && (mask != ProtoSkyMod.DEFAULT_MASK));

                    BlockPos logOrigin = this.maskOrigins.get(index);
                    assert logOrigin != null;

                    counter.total().add(logOrigin);
                    if (wasGraced)
                        counter.graced().add(logOrigin);
                    if (wasGenerated)
                        counter.vanilla().add(logOrigin);
                    if (wasGraced && wasGenerated)
                        counter.generated().add(logOrigin);

                    BlockPos origin = this.maskOrigins.peekLast();
                    assert origin != null;

                    ChunkPos center = this.getCenter();
                    if (!isSubset) {
                        ProtoSkyMod.LOGGER.warn(
                                "ChunkRegion ({} {}) attempted to generate {} at [{} {} {}]| wasGenerated={}, wasGraced={}| total={}, graced={}, vanilla={}, generated={}",
                                center.x, center.z,
                                referenceString,
                                origin.getX(), origin.getY(), origin.getZ(),
                                wasGenerated,
                                wasGraced,
                                counter.total().size(),
                                counter.graced().size(),
                                counter.vanilla().size(),
                                counter.generated().size()
                        );
                    } else {
                        BlockPos parentOrigin = this.maskOrigins.peekFirst();
                        assert parentOrigin != null;
                        ProtoSkyMod.LOGGER.warn(
                                "ChunkRegion ({} {}) attempted to generate {} at [{} {} {}] with parent at [{} {} {}]| wasGenerated={}, wasGraced={}| total={}, graced={}, vanilla={}, generated={}",
                                center.x, center.z,
                                referenceString,
                                origin.getX(), origin.getY(), origin.getZ(),
                                parentOrigin.getX(), parentOrigin.getY(), parentOrigin.getZ(),
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
        } catch (Throwable t) {
            ProtoSkyMod.LOGGER.error("Error while logging", t);
        }
    }
}
