package protosky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.ResourceType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protosky.datapack.ResourceReloader;
import protosky.datapack.ProtoSkySpawn;
import protosky.interfaces.FeatureWorldMask;

import java.util.*;

public class ProtoSkyMod implements ModInitializer {

    private static final Runnable NOOP = () -> {};
    public static final ResourceBundle PLACEHOLDERS;
    public static final Logger LOGGER;
    public static final Gson JSON_READER = new GsonBuilder().setLenient().disableHtmlEscaping().create();

    // CONSTANTS
    public static final String GRACES_TAG = "protosky_graces";
    public static final String OLD_STATUS_TAG = "protosky_old_status";
    public static final FeatureWorldMask EMPTY_MASK = new FeatureWorldMask(){};

    public static final FeatureWorldMask DEFAULT_MASK = new FeatureWorldMask() {

        @Override
        public boolean canGenerate(Double value) {
            return true;
        }

        @Override
        public boolean isReplaceable() {
            return true;
        }
    };

    // Globals

    public static final Map<RegistryKey<?>, FeatureWorldMask> baked_masks = new WeakHashMap<>();

    public static ProtoSkySpawn spawnInfo = new ProtoSkySpawn(null, null);
    public static Set<RegistryKey<World>> ignoredWorlds = new HashSet<>();

    public static Registry<PlacedFeature> placedFeatureRegistry = null;
    public static Registry<ConfiguredFeature<?, ?>> configuredFeatureRegistry = null;
    public static Registry<Feature<?>> featureRegistry = null;
    public static Registry<Structure> structureRegistry = null;


    // Initializer

    static {
        PLACEHOLDERS = ResourceBundle.getBundle("placeholders");
        LOGGER = LogManager.getLogger("ProtoSky");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Protosky %s build %s loaded, have a void day!".formatted(PLACEHOLDERS.getString("version"), PLACEHOLDERS.getString("build")));
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ResourceReloader());
    }

    public static Registry<PlacedFeature> getPlacedFeatureRegistry() {
        return getPlacedFeatureRegistry(null);
    }

    public static Registry<ConfiguredFeature<?, ?>> getConfiguredFeatureRegistry() {
        return getConfiguredFeatureRegistry(null);
    }

    public static Registry<Feature<?>> getFeatureRegistry() {
        return getFeatureRegistry(null);
    }

    public static Registry<Structure> getStructureRegistry() {
        return getStructureRegistry(null);
    }

    public static synchronized Registry<PlacedFeature> getPlacedFeatureRegistry(WorldView world) {
        if (placedFeatureRegistry == null && world != null)
            placedFeatureRegistry = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);
        return placedFeatureRegistry;
    }

    public static synchronized Registry<ConfiguredFeature<?, ?>> getConfiguredFeatureRegistry(WorldView world) {
        if (configuredFeatureRegistry == null && world != null)
            configuredFeatureRegistry = world.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE);
        return configuredFeatureRegistry;
    }

    public static synchronized Registry<Feature<?>> getFeatureRegistry(WorldView world) {
        if (featureRegistry == null && world != null)
            featureRegistry = world.getRegistryManager().get(RegistryKeys.FEATURE);
        return featureRegistry;
    }

    public static synchronized Registry<Structure> getStructureRegistry(WorldView world) {
        if (structureRegistry == null && world != null)
            structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        return structureRegistry;
    }
}
