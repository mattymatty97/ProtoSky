package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.interfaces.FeatureWorldMask;
import protosky.interfaces.GraceHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin {
    @Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth);

    @Shadow @Final private Chunk centerPos;
    @Unique
    private FeatureWorldMask mask = null;

    @Unique
    private Random chunkRandom = new Random();

    @Unique
    private final Set<BlockPos> blocks_to_rollback = new HashSet<>();
    @Unique
    private final Map<ProtoSkyMod.GraceConfig.BlockConfig, AtomicInteger> structure_map = new HashMap<>();


    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ServerWorld world, List<Chunk> chunks, ChunkStatus status, int placementRadius, CallbackInfo ci){
        ChunkPos pos = this.centerPos.getPos();
        long seed = world.getSeed() + pos.hashCode();
        chunkRandom = new Random(seed);
    }


    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkRegion;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;", shift = At.Shift.BEFORE))
    private void checkSetBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalRef<BlockState> forced_state){
        if (mask!=null) {
            Chunk chunk = ((WorldView) this).getChunk(pos);
            if (mask.canPlace(forced_state, chunkRandom, structure_map)) {
                ((GraceHolder) chunk).protoSky$putGracedBlock(pos, forced_state.get());
                blocks_to_rollback.remove(pos.toImmutable());
                return;
            } else {
                if (((GraceHolder) chunk).protoSky$getGracedBlocks().get(pos) != null)
                    ((GraceHolder) chunk).protoSky$putGracedBlock(pos, null);
            }
            blocks_to_rollback.add(pos.toImmutable());
        }
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void checkSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalRef<Entity> forced_entity){
        if (mask!=null) {
            Chunk chunk = ((WorldView) this).getChunk(entity.getBlockPos());
            if (mask.canSpawn(forced_entity, chunkRandom)) {
                ((GraceHolder) chunk).protoSky$putGracedEntity(forced_entity.get());
                return;
            }

            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setCurrentlyGeneratingStructureName", at=@At("HEAD"))
    private void trackFeature(@Nullable Supplier<String> structureName, CallbackInfo ci){
        structure_map.clear();
        if (structureName == null){
            this.mask = null;
            blocks_to_rollback.forEach((blockPos) -> {
                this.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 0 ,0);
            });
            blocks_to_rollback.clear();
        }else {
            String identifier = structureName.get();
            FeatureWorldMask mask = ProtoSkyMod.baked_masks.getOrDefault(identifier, ProtoSkyMod.EMPTY_MASK);
            if (mask.hasGraces(chunkRandom))
                this.mask = mask;
            else
                this.mask = ProtoSkyMod.EMPTY_MASK;
        }
    }
}
