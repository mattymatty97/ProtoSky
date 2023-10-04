package protosky.mixins.accessors;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PlacedFeature.class)
public interface PlacedFeatureAccessor {
    @Accessor
    RegistryEntry<ConfiguredFeature<?, ?>> getFeature();

    @Accessor
    List<PlacementModifier> getPlacementModifiers();
}
