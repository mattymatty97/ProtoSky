package protosky;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record ProtoSkySpawn(RegistryKey<World> spawnWorld, BlockPos spawnPos) {
}
