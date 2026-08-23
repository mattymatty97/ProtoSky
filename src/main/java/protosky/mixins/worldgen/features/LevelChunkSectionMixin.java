package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import protosky.ProtoSkyMod;
import protosky.ThreadLocals;
import protosky.interfaces.FeatureWorldMask;
import protosky.interfaces.GenerationMaskHolder;
import protosky.interfaces.GraceHolder;
import protosky.interfaces.SectionOfChunk;

@Mixin(LevelChunkSection.class)
public class LevelChunkSectionMixin {


    @Inject(method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "HEAD"))
    private void checkSetBlock(CallbackInfoReturnable<BlockState> cir,
                               @Local(argsOnly = true, name = "i") int x,
                               @Local(argsOnly = true, name = "j") int y,
                               @Local(argsOnly = true, name = "k") int z,
                               @Local(argsOnly = true, name = "blockState") BlockState state,
                               @Local(argsOnly = true, name = "blockState") LocalRef<BlockState> forced_state
    ) {
        WorldGenRegion region = ThreadLocals.currentRegion.get();
        if (region != null) {
            GenerationMaskHolder holder = ((GenerationMaskHolder) region);
            FeatureWorldMask mask = holder.protoSky$getMask();
            if (mask != null) {
                ChunkAccess chunk = ((SectionOfChunk) this).protoSky$getChunk();
                int y_offset = ((SectionOfChunk) this).protoSky$getYOffset();
                BlockPos pos = chunk.getPos().getBlockAt(x, y + y_offset, z);
                RandomSource random = ThreadLocals.graceRandom.get();
                if (random == null) {
                    ProtoSkyMod.LOGGER.warn("Missing random while placing block {} ({},{},{})", state.toString(), pos.getX(), pos.getY(), pos.getZ());
                    random = RandomSource.createNewThreadLocalInstance();
                }
                if (mask.canPlace(forced_state, random.nextDouble())) {
                    ((GraceHolder) chunk).protoSky$putGracedBlock(pos, forced_state.get());
                    holder.protoSky$updateRollbacks(pos, true);
                } else {
                    if (((GraceHolder) chunk).protoSky$getGracedBlocks().get(pos) != null)
                        ((GraceHolder) chunk).protoSky$putGracedBlock(pos, null);
                    holder.protoSky$updateRollbacks(pos, false);
                }
            }
        }
    }
}
