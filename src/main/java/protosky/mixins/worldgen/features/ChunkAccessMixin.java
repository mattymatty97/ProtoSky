package protosky.mixins.worldgen.features;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import protosky.interfaces.GraceHolder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements GraceHolder {
    @Unique
    private Map<BlockPos, BlockState> gracedBlockStates;

    @Unique
    private Set<CompoundTag> gracedEntities;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        gracedBlockStates = new ConcurrentHashMap<>();
        gracedEntities = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public Map<BlockPos, BlockState> protoSky$getGracedBlocks() {
        return gracedBlockStates;
    }

    @Override
    public void protoSky$putGracedBlock(BlockPos pos, BlockState state) {
        if (state != null)
            gracedBlockStates.put(pos.immutable(), state);
        else
            gracedBlockStates.remove(pos.immutable());
    }

    @Override
    public Set<CompoundTag> protoSky$getGracedEntities() {
        return gracedEntities;
    }

    @Override
    public void protoSky$putGracedEntity(Entity entity) {
        CompoundTag entity_nbt = new CompoundTag();
        if (entity.save(entity_nbt))
            gracedEntities.add(entity_nbt);
    }
}
