package protosky.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;

public interface GraceHolder {
    String ENTITY_TAG = "gracedEntities";
    String BLOCKSTATE_TAG = "gracedBlockStates";

    Map<BlockPos, BlockState> protoSky$getGracedBlocks();

    void protoSky$putGracedBlock(BlockPos pos, BlockState state);

    Set<CompoundTag> protoSky$getGracedEntities();

    void protoSky$putGracedEntity(Entity entity);
}
