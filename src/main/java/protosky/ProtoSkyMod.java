package protosky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protosky.datapack.ProtoSkySpawn;
import protosky.datapack.ResourceReloader;
import protosky.interfaces.FeatureWorldMask;

import java.util.*;

public class ProtoSkyMod implements ModInitializer {

    private static final Runnable NOOP = () -> {
    };
    public static final ResourceBundle PLACEHOLDERS;
    public static final Logger LOGGER;
    public static final Gson JSON_READER = new GsonBuilder().setLenient().disableHtmlEscaping().create();

    // CONSTANTS
    public static final String GRACES_TAG = "protosky_graces";
    public static final String OLD_STATUS_TAG = "protosky_old_status";

    public static final EnumSet<Heightmap.Types> CUSTOM_HEIGHTMAPS = EnumSet.of(Heightmap.Types.PROTO_SKY_VANILLA_OCEAN_FLOOR, Heightmap.Types.PROTO_SKY_VANILLA_WORLD_SURFACE);

    public static final FeatureWorldMask EMPTY_MASK = new FeatureWorldMask() {
    };


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

    public static final Map<ResourceKey<?>, FeatureWorldMask> baked_masks = new WeakHashMap<>();

    public static ProtoSkySpawn spawnInfo = new ProtoSkySpawn(null, null);
    public static Set<ResourceKey<Level>> ignoredWorlds = new HashSet<>();

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
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ResourceReloader());
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

    public static synchronized Registry<PlacedFeature> getPlacedFeatureRegistry(LevelReader world) {
        if (placedFeatureRegistry == null && world != null)
            placedFeatureRegistry = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        return placedFeatureRegistry;
    }

    public static synchronized Registry<ConfiguredFeature<?, ?>> getConfiguredFeatureRegistry(LevelReader world) {
        if (configuredFeatureRegistry == null && world != null)
            configuredFeatureRegistry = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        return configuredFeatureRegistry;
    }

    public static synchronized Registry<Feature<?>> getFeatureRegistry(LevelReader world) {
        if (featureRegistry == null && world != null)
            featureRegistry = world.registryAccess().registryOrThrow(Registries.FEATURE);
        return featureRegistry;
    }

    public static synchronized Registry<Structure> getStructureRegistry(LevelReader world) {
        if (structureRegistry == null && world != null)
            structureRegistry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return structureRegistry;
    }
}
