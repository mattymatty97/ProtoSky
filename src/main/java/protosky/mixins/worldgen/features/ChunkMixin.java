package protosky.mixins.worldgen.features;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.BlendingData;
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
    private Set<NbtCompound> gracedEntities ;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry biomeRegistry, long inhabitedTime, ChunkSection[] sectionArray, BlendingData blendingData, CallbackInfo ci){
        gracedBlockStates = new ConcurrentHashMap<>();

        //TODO remove debug blocks
        gracedBlockStates.put(pos.getStartPos(), Blocks.SEA_LANTERN.getDefaultState());
        gracedEntities = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public Map<BlockPos, BlockState> protoSky$getGracedBlocks() {
        return gracedBlockStates;
    }

    @Override
    public void protoSky$putGracedBlock(BlockPos pos, BlockState state) {
          if (state!=null)
            gracedBlockStates.put(pos.toImmutable(), state);
        else
            gracedBlockStates.remove(pos.toImmutable());
    }

    @Override
    public Set<NbtCompound> protoSky$getGracedEntities() {
        return ImmutableSet.copyOf(gracedEntities);
    }

    @Override
    public void protoSky$putGracedEntity(Entity entity) {
        NbtCompound entity_nbt = new NbtCompound();
        if (entity.saveNbt(entity_nbt))
            gracedEntities.add(entity_nbt);
    }
}
