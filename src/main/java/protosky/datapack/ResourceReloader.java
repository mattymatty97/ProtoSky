package protosky.datapack;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import joptsimple.internal.Strings;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import protosky.Debug;
import protosky.ProtoSkyMod;
import protosky.datapack.config.DebugConfig;
import protosky.datapack.config.GraceConfig;
import protosky.datapack.config.SpawnConfig;
import protosky.exceptions.DataPackException;
import protosky.interfaces.FeatureWorldMask;

import java.io.Reader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ResourceReloader implements SimpleSynchronousResourceReloadListener {
    private static final ResourceLocation MOD_IDENTIFIER = new ResourceLocation("protosky", "");
    public static final ResourceLocation GRACE_IDENTIFIER = MOD_IDENTIFIER.withPath("grace");
    public static final ResourceLocation SPAWN_IDENTIFIER = MOD_IDENTIFIER.withPath("spawn/forced.json");
    public static final ResourceLocation DEBUG_IDENTIFIER = MOD_IDENTIFIER.withPath("debug.json");
    public static final ResourceLocation IGNORED_WORLDS_IDENTIFIER = MOD_IDENTIFIER.withPath("world/ignored.json");

    /**
     * Bake the check for the specified entity Object
     *
     * @param entityConfig the config for the current check
     * @return the baked function to be checked against
     */
    private static Tuple<EntityType<?>, BiFunction<Double, LocalRef<Entity>, Boolean>> makeEntityCheck(String graceName, GraceConfig.EntityConfig entityConfig) {
        try {
            BiFunction<Double, LocalRef<Entity>, Boolean> currEntityCheck;
            EntityType<?> type;

            if (entityConfig.entity != null) {
                ResourceLocation id = ResourceLocation.tryParse(entityConfig.entity);
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id))
                    throw new DataPackException("Entity %s does not exist".formatted(entityConfig.entity));
                type = BuiltInRegistries.ENTITY_TYPE.get(id);
            } else {
                throw new DataPackException("Entity name is required");
            }

            //generate the probability check
            Function<Double, Boolean> probabilityCheck = ResourceReloader.getProbabilityCheck(entityConfig.probability);

            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currEntityCheck = (r, ref) -> probabilityCheck.apply(r);
            return new Tuple<>(type, currEntityCheck);
        } catch (DataPackException ex) {
            ProtoSkyMod.LOGGER.error("Error while baking entity check for {}: {}", graceName, ex.getMessage());
        } catch (Throwable ex) {
            ProtoSkyMod.LOGGER.error("Error while baking entity check for {}", graceName, ex);
        }
        return null;
    }

    /**
     * Bake the check for the specified BlockState Object
     *
     * @param blockConfig the config for the current check
     * @return the baked function to be checked against
     */
    private static Tuple<Block, BiFunction<Double, LocalRef<BlockState>, Boolean>> makeBlockCheck(String graceName, GraceConfig.BlockConfig blockConfig) {

        try {
            Block block;
            BiFunction<Double, LocalRef<BlockState>, Boolean> currBlockCheck;

            //if it has a name check
            if (blockConfig.block != null) {
                ResourceLocation id = ResourceLocation.tryParse(blockConfig.block);
                if (!BuiltInRegistries.BLOCK.containsKey(id))
                    throw new DataPackException("Block %s does not exist".formatted(blockConfig.block));
                block = BuiltInRegistries.BLOCK.get(id);
            } else {
                throw new DataPackException("Block name is required");
            }

            //generate the probability check
            Function<Double, Boolean> probabilityCheck = ResourceReloader.getProbabilityCheck(blockConfig.probability);

            //if there are blockstates to be checked
            Function<BlockState, Boolean> stateCheck;
            if (blockConfig.states != null) {
                //start with true if no blockstates have to be checked
                Function<BlockState, Boolean> tmpStateCheck = s -> true;
                //for each blockstate
                for (GraceConfig.BlockConfig.StateConfig stateConfig : blockConfig.states) {
                    Function<BlockState, Boolean> currStateCheck = tmpStateCheck;
                    //check if the blockstate beeing placed has the same value as the Config
                    Function<BlockState, Boolean> localStateCheck = blockState -> {
                        Collection<Property<?>> properties = blockState.getProperties();
                        Optional<Property<?>> optionalProperty = properties.stream().filter(p -> p.getName().equals(stateConfig.key)).findAny();
                        return optionalProperty.filter(property -> blockState.getValue(property).toString().equals(stateConfig.value.toString())).isPresent();
                    };
                    //combine the checks in AND with each other
                    tmpStateCheck = blockState -> currStateCheck.apply(blockState) && localStateCheck.apply(blockState);
                }
                stateCheck = tmpStateCheck;
            } else {
                stateCheck = null;
            }

            //the code for forcing a blockstate is commented because I was not able to make the castings work as intended
            Consumer<LocalRef<BlockState>> blockStateProcessor;
            if (blockConfig.forced_states != null) {

                List<Property.Value> to_set = new LinkedList<>();
                BlockState blockState = block.defaultBlockState();
                for (GraceConfig.BlockConfig.StateConfig stateConfig : blockConfig.forced_states) {

                    Property property = blockState.getProperties().stream().filter(p -> p.getName().equals(stateConfig.key)).findAny().orElse(null);
                    if (property == null)
                        throw new DataPackException("Property %s is not a property of block %s".formatted(stateConfig.key, blockConfig.block));

                    Optional value = property.getValue(stateConfig.value.toString());
                    if (value.isEmpty())
                        throw new DataPackException("Property %s is not a valid value for %s ".formatted(stateConfig.value, stateConfig.key));

                    to_set.add(property.value((Comparable) value.get()));

                }

                blockStateProcessor = ref -> {
                    BlockState state = ref.get();
                    for (Property.Value value : to_set) {
                        state = state.setValue(value.property(), value.value());
                    }
                    ref.set(state);
                };
            } else {
                blockStateProcessor = null;
            }


            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currBlockCheck = (r, ref) -> {
                boolean result = (stateCheck != null ? stateCheck.apply(ref.get()) : true)
                        && probabilityCheck.apply(r);
                if (result && blockStateProcessor != null)
                    blockStateProcessor.accept(ref);
                return result;
            };
            return new Tuple<>(block, currBlockCheck);
        } catch (DataPackException dpe) {
            ProtoSkyMod.LOGGER.error("Error while baking block check for {}: {}", graceName, dpe.getMessage());
        } catch (Throwable ex) {
            ProtoSkyMod.LOGGER.error("Error while baking block check for {}", graceName, ex);
        }
        return null;
    }

    /**
     * prepare the probability check given the Double value
     *
     * @param probability the probability to be checked against. range -> [0.0, 1.0]
     * @return the probability check
     */
    private static Function<Double, Boolean> getProbabilityCheck(Double probability) {
        return getProbabilityCheck(probability, 1.0);
    }

    /**
     * prepare the probability check given the Double value
     *
     * @param probability        the probability to be checked against. range -> [0.0, 1.0]
     * @param defaultProbability the value in case of null. range -> [0.0, 1.0]
     * @return the probability check
     */
    private static Function<Double, Boolean> getProbabilityCheck(Double probability, double defaultProbability) {
        Function<Double, Boolean> probabilityCheck;
        double finalProbability;

        finalProbability = Objects.requireNonNullElse(probability, defaultProbability);

        if (finalProbability < 1.0 && finalProbability > 0.0)
            probabilityCheck = value -> value < finalProbability;
        else if (finalProbability <= 0.0)
            probabilityCheck = value -> false;
        else
            probabilityCheck = r -> true;

        return probabilityCheck;
    }

    @Override
    public ResourceLocation getFabricId() {
        return MOD_IDENTIFIER;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        //forget old data
        ProtoSkyMod.baked_masks.clear();
        ProtoSkyMod.spawnInfo = new ProtoSkySpawn(null, null);
        ProtoSkyMod.ignoredWorlds.clear();

        ProtoSkyMod.placedFeatureRegistry = null;
        ProtoSkyMod.configuredFeatureRegistry = null;
        ProtoSkyMod.featureRegistry = null;
        ProtoSkyMod.structureRegistry = null;

        Debug.chunkOriginBlock = null;
        Debug.attemptMap.clear();
        Debug.anyAttempt = false;


        //get all json files in the grace tree
        Map<ResourceLocation, Resource> graceResourceMap = manager.listResources(GRACE_IDENTIFIER.getPath(), id -> id.getNamespace().equals(MOD_IDENTIFIER.getNamespace()) && id.getPath().endsWith(".json"));

        //parse each json
        for (Map.Entry<ResourceLocation, Resource> entry : graceResourceMap.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                // cast the json to the Config class
                GraceConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, GraceConfig.class);

                if (config.override == null)
                    config.override = new GraceConfig.Override();

                //obtain this config name/key
                ResourceKey<?> key = null;

                //generate the name based on the path the file was
                String currPath = entry.getKey().getPath();

                //remove the prefix from the path
                String main_path = currPath.substring(GRACE_IDENTIFIER.getPath().length());
                //remove extra / if present
                if (main_path.startsWith("/"))
                    main_path = main_path.substring(1);

                //obtain the tree structure
                String name = null;
                String[] sections = main_path.split("/");
                if (sections.length > 1 || (config.override.namespace != null && config.override.name != null && config.override.path != null)) {
                    //first folder is the namespace
                    String namespace = config.override.namespace != null ? config.override.namespace : sections[0];

                    //last section is the filename, we trim it from the extension to get the vanilla resource name
                    String resourceName = config.override.name != null ? config.override.name : sections[sections.length - 1].replace(".json", "");

                    //generate the resource path from the remaining parts
                    List<String> subSections = Arrays.stream(sections).skip(1).limit(sections.length - 2).toList();
                    String path = config.override.path != null ? config.override.path : Strings.join(subSections, "/");

                    ResourceLocation registryIdentifier = new ResourceLocation(config.override.registry_namespace != null ? config.override.registry_namespace : "minecraft", path);
                    ResourceLocation resourceIdentifier = new ResourceLocation(namespace, resourceName);

                    key = ResourceKey.create(ResourceKey.createRegistryKey(registryIdentifier), resourceIdentifier);
                    name = key.toString();
                }

                //do not bother if it was already set
                if (key != null && !ProtoSkyMod.baked_masks.containsKey(key)) {

                    //bake the probability check
                    Function<Double, Boolean> mainCheck = ResourceReloader.getProbabilityCheck(config.probability, 0);

                    //if there is an entity section iterate over it
                    BiFunction<Double, LocalRef<Entity>, Boolean> entityCheck;
                    if (config.entities != null) {
                        //start with an always false check to fail in case of empty list
                        Map<EntityType, BiFunction<Double, LocalRef<Entity>, Boolean>> checkMap = new HashMap<>();

                        for (GraceConfig.EntityConfig entityConfig : config.entities) {
                            Tuple<EntityType<?>, BiFunction<Double, LocalRef<Entity>, Boolean>> currEntityCheck = ResourceReloader.makeEntityCheck(name, entityConfig);
                            //append the new check in or with the previous ones
                            if (currEntityCheck != null)
                                checkMap.put(currEntityCheck.getA(), currEntityCheck.getB());
                        }
                        entityCheck = (r, ref) -> {
                            BiFunction<Double, LocalRef<Entity>, Boolean> func = checkMap.get(ref.get().getType());
                            if (func != null)
                                return func.apply(r, ref);
                            return false;
                        };
                    } else {
                        entityCheck = (r, ref) -> true;
                    }

                    //if there is a block check
                    BiFunction<Double, LocalRef<BlockState>, Boolean> blockCheck;
                    if (config.blocks != null) {
                        Map<Block, BiFunction<Double, LocalRef<BlockState>, Boolean>> checkMap = new HashMap<>();
                        //start with an always false check to fail in case of empty list
                        for (GraceConfig.BlockConfig blockConfig : config.blocks) {
                            Tuple<Block, BiFunction<Double, LocalRef<BlockState>, Boolean>> currBlockCheck = ResourceReloader.makeBlockCheck(name, blockConfig);
                            //append the new check in or with the previous ones
                            if (currBlockCheck != null)
                                checkMap.put(currBlockCheck.getA(), currBlockCheck.getB());
                        }
                        blockCheck = (r, ref) -> {
                            BiFunction<Double, LocalRef<BlockState>, Boolean> func = checkMap.get(ref.get().getBlock());
                            if (func != null)
                                return func.apply(r, ref);
                            return false;
                        };
                    } else {
                        blockCheck = (r, ref) -> true;
                    }

                    //combine all the backed checks into the Mask Class
                    FeatureWorldMask mask = new FeatureWorldMask() {
                        @Override
                        public boolean canGenerate(Double value) {
                            return mainCheck.apply(value);
                        }

                        @Override
                        public boolean canPlace(LocalRef<BlockState> blockState, Double value) {
                            return blockCheck.apply(value, blockState);
                        }

                        @Override
                        public boolean canSpawn(LocalRef<Entity> entity, Double value) {
                            return entityCheck.apply(value, entity);
                        }
                    };

                    ProtoSkyMod.baked_masks.put(key, mask);
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading resource json {}", entry.getKey().toString(), t);
            }
        }

        //get the json file in the spawn tree
        List<Resource> spawnResources = manager.getResourceStack(SPAWN_IDENTIFIER);

        //parse each json ( should be only one )
        for (Resource resource : spawnResources) {
            try (Reader reader = resource.openAsReader()) {
                // cast the json to the Config class
                SpawnConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, SpawnConfig.class);

                ResourceKey<Level> worldKey = null;
                BlockPos spawnPos = null;

                if (config.worldKey != null) {
                    ResourceLocation worldId = ResourceLocation.tryParse(config.worldKey);
                    if (worldId != null) {
                        worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
                    } else {
                        ProtoSkyMod.LOGGER.warn("Malformed spawn world string: {}", config.worldKey);
                    }
                }

                if (config.spawnPos != null && config.spawnPos.size() == 3) {
                    spawnPos = new BlockPos(config.spawnPos.getInt(0), config.spawnPos.getInt(1), config.spawnPos.getInt(2));
                }

                ProtoSkyMod.spawnInfo = new ProtoSkySpawn(worldKey, spawnPos);
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading spawn resource json {}", SPAWN_IDENTIFIER.toString(), t);
            }
        }

        //get the json file in the world tree
        List<Resource> worldResources = manager.getResourceStack(IGNORED_WORLDS_IDENTIFIER);

        //parse each json ( should be only one )
        for (Resource resource : worldResources) {
            try (Reader reader = resource.openAsReader()) {
                // cast the json to the Config class
                String[] config = ProtoSkyMod.JSON_READER.fromJson(reader, String[].class);

                for (String worldkey : config) {
                    ProtoSkyMod.ignoredWorlds.add(ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(worldkey)));
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading debug resource json {}", IGNORED_WORLDS_IDENTIFIER.toString(), t);
            }
        }

        //get the json file in the debug tree
        List<Resource> debugResources = manager.getResourceStack(DEBUG_IDENTIFIER);

        //parse each json ( should be only one )
        for (Resource resource : debugResources) {
            try (Reader reader = resource.openAsReader()) {
                // cast the json to the Config class
                DebugConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, DebugConfig.class);

                if (config.chunkOriginBlock != null) {
                    ResourceLocation id = ResourceLocation.tryParse(config.chunkOriginBlock);
                    if (BuiltInRegistries.BLOCK.containsKey(id))
                        Debug.chunkOriginBlock = BuiltInRegistries.BLOCK.get(id);
                    else
                        ProtoSkyMod.LOGGER.warn("Block {} for chunkOriginBlock is invalid", config.chunkOriginBlock);
                }

                if (config.loggingFeatures != null) {
                    for (String feature : config.loggingFeatures) {
                        Debug.attemptMap.put(feature, new Debug.AttemptCounter());
                        if (feature.equals("*"))
                            Debug.anyAttempt = true;
                    }
                }

                if (config.preventChunkSave != null) {
                    Debug.preventSave = config.preventChunkSave;
                }

            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading debug resource json {}", DEBUG_IDENTIFIER.toString(), t);
            }
        }
    }
}
