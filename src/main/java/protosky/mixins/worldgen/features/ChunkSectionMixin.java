package protosky.mixins.worldgen.features;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
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

@Mixin(ChunkSection.class)
public class ChunkSectionMixin {


    @Inject(method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At(value = "HEAD"))
    private void checkSetBlock(int x, int y, int z, BlockState state, boolean lock, CallbackInfoReturnable<BlockState> cir,
                               @Local(argsOnly = true) LocalRef<BlockState> forced_state){
        ChunkRegion region = ThreadLocals.currentRegion.get();
        if (region!=null) {
            GenerationMaskHolder holder = ((GenerationMaskHolder)region);
            FeatureWorldMask mask = holder.protoSky$getMask();
            if (mask != null) {
                Chunk chunk = ((SectionOfChunk)this).protoSky$getChunk();
                int y_offset = ((SectionOfChunk)this).protoSky$getYOffset(()->chunk.getHeightLimitView().sectionIndexToCoord(((SectionOfChunk)this).protoSky$getSectionIndex()) * 16);
                BlockPos pos = chunk.getPos().getBlockPos(x, y + y_offset, z);
                Random random = ThreadLocals.graceRandom.get();
                if (random == null) {
                    ProtoSkyMod.LOGGER.warn("Missing random while placing block {} ({},{},{})", state.toString(), pos.getX(), pos.getY(), pos.getZ());
                    random = Random.createLocal();
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
