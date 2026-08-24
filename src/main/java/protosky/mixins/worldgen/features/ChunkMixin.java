package protosky.mixins.worldgen.features;

import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.Entity;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;
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

@Mixin(Chunk.class)
public abstract class ChunkMixin implements GraceHolder {
    @Unique
    private Map<BlockPos, BlockState> gracedBlockStates;

    @Unique
    private Set<NbtCompound> gracedEntities;

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
            gracedBlockStates.put(pos.toImmutable(), state);
        else
            gracedBlockStates.remove(pos.toImmutable());
    }

    @Override
    public Set<NbtCompound> protoSky$getGracedEntities() {
        return gracedEntities;
    }

    @Override
    public void protoSky$putGracedEntity(Entity entity) {
        NbtCompound entity_nbt = new NbtCompound();
        if (entity.saveNbt(entity_nbt))
            gracedEntities.add(entity_nbt);
    }
}
