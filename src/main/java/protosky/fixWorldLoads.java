package protosky;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import protosky.stuctures.StructureHelper;

import static protosky.ProtoSkySettings.LOGGER;

public class fixWorldLoads implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, isClient) -> {
            StructureHelper.ran = false;
        });
    }
}
