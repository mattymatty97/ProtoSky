package protosky;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protosky.stuctures.StructureHelper;

public class ProtoSkyMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("ProtoSky");
    @Override
    public void onInitialize() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, isClient) -> {
            StructureHelper.ran = false;
        });
    }
}
