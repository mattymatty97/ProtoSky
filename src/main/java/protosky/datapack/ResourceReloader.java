package protosky.datapack;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import joptsimple.internal.Strings;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import protosky.Debug;
import protosky.ProtoSkyMod;
import protosky.datapack.config.DebugConfig;
import protosky.datapack.config.GraceConfig;
import protosky.datapack.config.SpawnConfig;
import protosky.exceptions.DataPackException;
import protosky.interfaces.FeatureWorldMask;
import protosky.mixins.accessors.StateAccessor;

import javax.swing.text.html.Option;
import javax.xml.crypto.Data;
import java.io.Reader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unchecked","rawtypes"})
public class ResourceReloader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier MOD_IDENTIFIER = new Identifier("protosky", "");
    public static final Identifier GRACE_IDENTIFIER = MOD_IDENTIFIER.withPath("grace");
    public static final Identifier SPAWN_IDENTIFIER = MOD_IDENTIFIER.withPath("spawn/forced.json");
    public static final Identifier DEBUG_IDENTIFIER = MOD_IDENTIFIER.withPath("debug.json");
    public static final Identifier IGNORED_WORLDS_IDENTIFIER = MOD_IDENTIFIER.withPath("world/ignored.json");

    /**
     * Bake the check for the specified entity Object
     *
     * @param entityConfig the config for the current check
     * @return the baked function to be checked against
     */
    @NotNull
    private static BiFunction<Double, LocalRef<Entity>, Boolean> makeEntityCheck(String graceName, GraceConfig.EntityConfig entityConfig) {
        try {
            BiFunction<Double, LocalRef<Entity>, Boolean> currEntityCheck;

            //if it has a name check
            Function<EntityType<?>, Boolean> nameCheck;
            if (entityConfig.entity != null) {
                Identifier id = Identifier.tryParse(entityConfig.entity);
                if (!Registries.ENTITY_TYPE.containsId(id))
                    throw new DataPackException("Entity %s does not exist".formatted(entityConfig.entity));
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                //check if the entity id matches the one in the config
                nameCheck = type1 -> type1.equals(type);
            } else {
                nameCheck = null;
            }

            //generate the probability check
            Function<Double, Boolean> probabilityCheck = ResourceReloader.getProbabilityCheck(entityConfig.probability);

            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currEntityCheck = (r, ref) -> (nameCheck != null ? nameCheck.apply(ref.get().getType()) : true)
                    && probabilityCheck.apply(r);
            return currEntityCheck;
        } catch (DataPackException ex) {
            ProtoSkyMod.LOGGER.error("Error while baking entity check for {}: {}", graceName, ex.getMessage());
        } catch (Throwable ex) {
            ProtoSkyMod.LOGGER.error("Error while baking entity check for {}", graceName, ex);
        }
        return (a, b) -> false;
    }

    /**
     * Bake the check for the specified BlockState Object
     *
     * @param blockConfig the config for the current check
     * @return the baked function to be checked against
     */
    @NotNull
    private static BiFunction<Double, LocalRef<BlockState>, Boolean> makeBlockCheck(String graceName, GraceConfig.BlockConfig blockConfig) {

        try {
            Block block;
            BiFunction<Double, LocalRef<BlockState>, Boolean> currBlockCheck;

            //if it has a name check
            Function<Block, Boolean> nameCheck;
            if (blockConfig.block != null) {
                Identifier id = Identifier.tryParse(blockConfig.block);
                if (!Registries.BLOCK.containsId(id))
                    throw new DataPackException("Block %s does not exist".formatted(blockConfig.block));
                block = Registries.BLOCK.get(id);
                //check if the block id matches the one in the config
                nameCheck = block1 -> block1.equals(block);
            } else {
                block = null;
                nameCheck = null;
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
                        return optionalProperty.filter(property -> blockState.get(property).toString().equals(stateConfig.value.toString())).isPresent();
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
                if (block == null)
                    throw new DataPackException("Forced states can only be used if you specify the block too");

                List<Property.Value> to_set = new LinkedList<>();
                BlockState blockState = block.getDefaultState();
                for (GraceConfig.BlockConfig.StateConfig stateConfig : blockConfig.forced_states) {

                    Property property = blockState.getProperties().stream().filter(p -> p.getName().equals(stateConfig.key)).findAny().orElse(null);
                    if (property == null)
                        throw new DataPackException("Property %s is not a property of block %s".formatted(stateConfig.key,blockConfig.block));

                    Optional value = property.parse(stateConfig.value.toString());
                    if (value.isEmpty())
                        throw new DataPackException("Property %s is not a valid value for %s ".formatted(stateConfig.value, stateConfig.key));

                    to_set.add(property.createValue((Comparable)value.get()));

                }

                blockStateProcessor = ref -> {
                    BlockState state = ref.get();
                    for(Property.Value value : to_set){
                        state = state.with(value.property(), value.value());
                    }
                    ref.set(state);
                };
            } else {
                blockStateProcessor = null;
            }


            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currBlockCheck = (r, ref) -> {
                boolean result = (nameCheck != null ? nameCheck.apply(ref.get().getBlock()) : true)
                        && (stateCheck != null ? stateCheck.apply(ref.get()) : true)
                        && probabilityCheck.apply(r);
                if (result && blockStateProcessor != null)
                                 blockStateProcessor.accept(ref);
                return result;
            };
            return currBlockCheck;
        } catch (DataPackException dpe){
            ProtoSkyMod.LOGGER.error("Error while baking block check for {}: {}", graceName, dpe.getMessage());
        }
        catch (Throwable ex) {
            ProtoSkyMod.LOGGER.error("Error while baking block check for {}", graceName, ex);
        }
        return (a, b) -> false;
    }

    /**
     * prepare the probability check given the Double value
     *
     * @param probability the probability to be checked against. range -> [0.0, 1.0]
     * @return the probability check
     */
    private static Function<Double, Boolean> getProbabilityCheck(Double probability) {
        Function<Double, Boolean> probabilityCheck;
        if (probability != null) {
            if (probability < 1.0 && probability > 0.0)
                probabilityCheck = value -> value < probability;
            else if (probability <= 0.0)
                probabilityCheck = value -> false;
            else
                probabilityCheck = r -> true;
        } else {
            probabilityCheck = r -> true;
        }
        return probabilityCheck;
    }

    @Override
    public Identifier getFabricId() {
        return MOD_IDENTIFIER;
    }

    @Override
    public void reload(ResourceManager manager) {
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
        Map<Identifier, Resource> graceResourceMap = manager.findResources(GRACE_IDENTIFIER.getPath(), id -> id.getPath().endsWith(".json"));

        //parse each json
        for (Map.Entry<Identifier, Resource> entry : graceResourceMap.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                // cast the json to the Config class
                GraceConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, GraceConfig.class);

                //obtain this config name/key
                String name = config.override_name;
                RegistryKey<?> key = null;
                if (name == null || name.isEmpty() || name.isBlank()) {
                    //generate the name based on the path the file was
                    String currPath = entry.getKey().getPath();
                    //remove the prefix from the path
                    String main_path = currPath.substring(GRACE_IDENTIFIER.getPath().length());
                    //remove extra / if present
                    if (main_path.startsWith("/"))
                        main_path = main_path.substring(1);
                    //obtain the tree structure
                    String[] sections = main_path.split("/");
                    if (sections.length > 1) {
                        //first folder is the vanilla namespace
                        String namespace = sections[0];
                        //last section is the filename, we trim it from the extension to get the vanilla resource name
                        String resourceName = sections[sections.length - 1].replace(".json", "");
                        //generate the vanilla resource path from the remaining parts
                        List<String> subSections = Arrays.stream(sections).skip(1).limit(sections.length - 2).toList();
                        Identifier registry = new Identifier(namespace, Strings.join(subSections, "/"));
                        Identifier resource = new Identifier(namespace, resourceName);
                        key = RegistryKey.of(RegistryKey.ofRegistry(registry), resource);
                        name = key.toString();
                    }
                }

                //do not bother if it was already set
                if (key != null && !ProtoSkyMod.baked_masks.containsKey(key)) {

                    //bake the probability check
                    Function<Double, Boolean> mainCheck = ResourceReloader.getProbabilityCheck(config.probability);

                    //if there is an entity section iterate over it
                    BiFunction<Double, LocalRef<Entity>, Boolean> entityCheck;
                    if (config.entities != null) {
                        //start with an always false check to fail in case of empty list
                        BiFunction<Double, LocalRef<Entity>, Boolean> tmpEntityCheck = (r, ref) -> false;
                        for (GraceConfig.EntityConfig entityConfig : config.entities) {
                            BiFunction<Double, LocalRef<Entity>, Boolean> oldEntityCheck = tmpEntityCheck;
                            BiFunction<Double, LocalRef<Entity>, Boolean> currEntityCheck = ResourceReloader.makeEntityCheck(name, entityConfig);
                            //append the new check in or with the previous ones
                            tmpEntityCheck = (r, ref) -> oldEntityCheck.apply(r, ref) || currEntityCheck.apply(r, ref);
                        }
                        entityCheck = tmpEntityCheck;
                    } else {
                        entityCheck = (r, ref) -> true;
                    }

                    //if there is a block check
                    BiFunction<Double, LocalRef<BlockState>, Boolean> blockCheck;
                    if (config.blocks != null) {
                        //start with an always false check to fail in case of empty list
                        BiFunction<Double, LocalRef<BlockState>, Boolean> tmpBlockCheck = (r, ref) -> false;
                        for (GraceConfig.BlockConfig blockConfig : config.blocks) {
                            BiFunction<Double, LocalRef<BlockState>, Boolean> oldBlockCheck = tmpBlockCheck;
                            BiFunction<Double, LocalRef<BlockState>, Boolean> currBlockCheck = ResourceReloader.makeBlockCheck(name, blockConfig);
                            //append the new check in or with the previous ones
                            tmpBlockCheck = (r, ref) -> oldBlockCheck.apply(r, ref) || currBlockCheck.apply(r, ref);
                        }
                        blockCheck = tmpBlockCheck;
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

                    //and add the mask to the map
                    ProtoSkyMod.baked_masks.put(key, mask);
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading resource json " + entry.getKey().toString(), t);
            }
        }

        //get the json file in the spawn tree
        Map<Identifier, Resource> spawnResourceMap = manager.findResources(SPAWN_IDENTIFIER.getPath(), id -> true);

        //parse each json ( should be only one )
        for (Map.Entry<Identifier, Resource> entry : spawnResourceMap.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                // cast the json to the Config class
                if (entry.getKey().getPath().equals(SPAWN_IDENTIFIER.getPath())) {
                    SpawnConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, SpawnConfig.class);

                    RegistryKey<World> worldKey = null;
                    BlockPos spawnPos = null;

                    if (config.worldKey != null) {
                        Identifier worldId = Identifier.tryParse(config.worldKey);
                        if (worldId != null) {
                            worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
                        } else {
                            ProtoSkyMod.LOGGER.warn("Malformed spawn world string: {}", config.worldKey);
                        }
                    }

                    if (config.spawnPos != null && config.spawnPos.size() == 3) {
                        spawnPos = new BlockPos(config.spawnPos.getInt(0), config.spawnPos.getInt(1), config.spawnPos.getInt(2));
                    }

                    ProtoSkyMod.spawnInfo = new ProtoSkySpawn(worldKey, spawnPos);
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading spawn resource json " + entry.getKey().toString(), t);
            }
        }

        //get the json file in the world tree
        Map<Identifier, Resource> worldResourceMap = manager.findResources(IGNORED_WORLDS_IDENTIFIER.getPath(), id -> true);

        //parse each json ( should be only one )
        for (Map.Entry<Identifier, Resource> entry : worldResourceMap.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                // cast the json to the Config class
                if (entry.getKey().getPath().equals(IGNORED_WORLDS_IDENTIFIER.getPath())) {
                    String[] config = ProtoSkyMod.JSON_READER.fromJson(reader, String[].class);

                    for (String worldkey : config) {
                        ProtoSkyMod.ignoredWorlds.add(RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(worldkey)));
                    }
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading debug resource json " + entry.getKey().toString(), t);
            }
        }

        //get the json file in the debug tree
        Map<Identifier, Resource> debugResourceMap = manager.findResources(DEBUG_IDENTIFIER.getPath(), id -> true);

        //parse each json ( should be only one )
        for (Map.Entry<Identifier, Resource> entry : debugResourceMap.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                // cast the json to the Config class
                if (entry.getKey().getPath().equals(DEBUG_IDENTIFIER.getPath())) {
                    DebugConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, DebugConfig.class);

                    if (config.chunkOriginBlock != null) {
                        Identifier id = Identifier.tryParse(config.chunkOriginBlock);
                        if (Registries.BLOCK.containsId(id))
                            Debug.chunkOriginBlock = Registries.BLOCK.get(id);
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

                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading debug resource json " + entry.getKey().toString(), t);
            }
        }
    }
}
