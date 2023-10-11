package protosky.mixins.worldgen.features;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import protosky.interfaces.GraceHolder;

import java.util.Map;
import java.util.Set;

@Mixin(WrapperProtoChunk.class)
public class WrappedProtoChunkMixin implements GraceHolder {

    @Shadow @Final private WorldChunk wrapped;

    @Override
    public Map<BlockPos, BlockState> protoSky$getGracedBlocks() {
        return ((GraceHolder)wrapped).protoSky$getGracedBlocks();
    }

    @Override
    public void protoSky$putGracedBlock(BlockPos pos, BlockState state) {
        ((GraceHolder)wrapped).protoSky$putGracedBlock(pos, state);
    }

    @Override
    public Set<NbtCompound> protoSky$getGracedEntities() {
        return ((GraceHolder)wrapped).protoSky$getGracedEntities();
    }

    @Override
    public void protoSky$putGracedEntity(Entity entity) {
        ((GraceHolder)wrapped).protoSky$putGracedEntity(entity);
    }
}
