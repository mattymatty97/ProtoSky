package protosky.mixins.worldgen.features;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import protosky.interfaces.GraceHolder;

import java.util.Map;
import java.util.Set;

@Mixin(ImposterProtoChunk.class)
public class WrappedProtoChunkMixin implements GraceHolder {

    @Shadow
    @Final
    private LevelChunk wrapped;

    @Override
    public Map<BlockPos, BlockState> protoSky$getGracedBlocks() {
        return ((GraceHolder) wrapped).protoSky$getGracedBlocks();
    }

    @Override
    public void protoSky$putGracedBlock(BlockPos pos, BlockState state) {
        ((GraceHolder) wrapped).protoSky$putGracedBlock(pos, state);
    }

    @Override
    public Set<CompoundTag> protoSky$getGracedEntities() {
        return ((GraceHolder) wrapped).protoSky$getGracedEntities();
    }

    @Override
    public void protoSky$putGracedEntity(Entity entity) {
        ((GraceHolder) wrapped).protoSky$putGracedEntity(entity);
    }
}
