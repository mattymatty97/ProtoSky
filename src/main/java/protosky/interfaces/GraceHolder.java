package protosky.interfaces;

import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.Entity;
import net.minecraft.block.BlockState;

import java.util.Map;
import java.util.Set;

public interface GraceHolder {
    String ENTITY_TAG = "gracedEntities";
    String BLOCKSTATE_TAG = "gracedBlockStates";

    Map<BlockPos, BlockState> protoSky$getGracedBlocks();

    void protoSky$putGracedBlock(BlockPos pos, BlockState state);

    Set<NbtCompound> protoSky$getGracedEntities();

    void protoSky$putGracedEntity(Entity entity);
}
