package protosky.datapack;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
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
    private static Pair<EntityType<?>, BiFunction<Double, LocalRef<Entity>, Boolean>> makeEntityCheck(String graceName, GraceConfig.EntityConfig entityConfig) {
        try {
            BiFunction<Double, LocalRef<Entity>, Boolean> currEntityCheck;
            EntityType<?> type;

            if (entityConfig.entity != null) {
                Identifier id = Identifier.tryParse(entityConfig.entity);
                if (!Registries.ENTITY_TYPE.containsId(id))
                    throw new DataPackException("Entity %s does not exist".formatted(entityConfig.entity));
                type = Registries.ENTITY_TYPE.get(id);
            } else {
                throw new DataPackException("Entity name is required");
            }

            //generate the probability check
            Function<Double, Boolean> probabilityCheck = ResourceReloader.getProbabilityCheck(entityConfig.probability);

            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currEntityCheck = (r, ref) -> probabilityCheck.apply(r);
            return new Pair<>(type, currEntityCheck);
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
    private static Pair<Block, BiFunction<Double, LocalRef<BlockState>, Boolean>> makeBlockCheck(String graceName, GraceConfig.BlockConfig blockConfig) {

        try {
            Block block;
            BiFunction<Double, LocalRef<BlockState>, Boolean> currBlockCheck;

            //if it has a name check
            if (blockConfig.block != null) {
                Identifier id = Identifier.tryParse(blockConfig.block);
                if (!Registries.BLOCK.containsId(id))
                    throw new DataPackException("Block %s does not exist".formatted(blockConfig.block));
                block = Registries.BLOCK.get(id);
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
                boolean result = (stateCheck != null ? stateCheck.apply(ref.get()) : true)
                        && probabilityCheck.apply(r);
                if (result && blockStateProcessor != null)
                                 blockStateProcessor.accept(ref);
                return result;
            };
            return new Pair<>(block, currBlockCheck);
        } catch (DataPackException dpe){
            ProtoSkyMod.LOGGER.error("Error while baking block check for {}: {}", graceName, dpe.getMessage());
        }
        catch (Throwable ex) {
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
        Map<Identifier, Resource> graceResourceMap = manager.findResources(GRACE_IDENTIFIER.getPath(), id -> id.getNamespace().equals(MOD_IDENTIFIER.getNamespace()) && id.getPath().endsWith(".json"));

        //parse each json
        for (Map.Entry<Identifier, Resource> entry : graceResourceMap.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                // cast the json to the Config class
                GraceConfig config = ProtoSkyMod.JSON_READER.fromJson(reader, GraceConfig.class);

                //obtain this config name/key
                Collection<RegistryKey<?>> keys = new ArrayList<>();
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
                if (sections.length > 1) {
                    //first folder is the vanilla namespace
                    String namespace = sections[0];
                    //last section is the filename, we trim it from the extension to get the vanilla resource name
                    String resourceName = sections[sections.length - 1].replace(".json", "");
                    //generate the vanilla resource path from the remaining parts
                    List<String> subSections = Arrays.stream(sections).skip(1).limit(sections.length - 2).toList();
                    String path = Strings.join(subSections, "/");
                    Identifier resource = new Identifier(namespace, resourceName);
                    Identifier m_registry = new Identifier(namespace, path);
                    name = RegistryKey.of(RegistryKey.ofRegistry(m_registry), resource).toString();
                    for(String n : manager.getAllNamespaces()){
                        Identifier registry = new Identifier(n, path);
                        RegistryKey<?> key = RegistryKey.of(RegistryKey.ofRegistry(registry), resource);
                        keys.add(key);
                    }
                }

                //do not bother if it was already set
                if (name != null && ProtoSkyMod.baked_masks.keySet().stream().noneMatch(keys::contains)) {

                    //bake the probability check
                    Function<Double, Boolean> mainCheck = ResourceReloader.getProbabilityCheck(config.probability);

                    //if there is an entity section iterate over it
                    BiFunction<Double, LocalRef<Entity>, Boolean> entityCheck;
                    if (config.entities != null) {
                        //start with an always false check to fail in case of empty list
                        Map<EntityType,BiFunction<Double, LocalRef<Entity>, Boolean>> checkMap = new HashMap<>();

                        for (GraceConfig.EntityConfig entityConfig : config.entities) {
                            Pair<EntityType<?>, BiFunction<Double, LocalRef<Entity>, Boolean>> currEntityCheck = ResourceReloader.makeEntityCheck(name, entityConfig);
                            //append the new check in or with the previous ones
                            if (currEntityCheck != null)
                                checkMap.put(currEntityCheck.getLeft(), currEntityCheck.getRight());
                        }
                        entityCheck = (r, ref) -> {
                            BiFunction<Double, LocalRef<Entity>, Boolean> func = checkMap.get(ref.get().getType());
                            if (func != null)
                                return func.apply(r,ref);
                            return false;
                        };
                    } else {
                        entityCheck = (r, ref) -> true;
                    }

                    //if there is a block check
                    BiFunction<Double, LocalRef<BlockState>, Boolean> blockCheck;
                    if (config.blocks != null) {
                        Map<Block,BiFunction<Double, LocalRef<BlockState>, Boolean>> checkMap = new HashMap<>();
                        //start with an always false check to fail in case of empty list
                        for (GraceConfig.BlockConfig blockConfig : config.blocks) {
                            Pair<Block,BiFunction<Double, LocalRef<BlockState>, Boolean>> currBlockCheck = ResourceReloader.makeBlockCheck(name, blockConfig);
                            //append the new check in or with the previous ones
                            if (currBlockCheck != null)
                                checkMap.put(currBlockCheck.getLeft(),currBlockCheck.getRight());
                        }
                        blockCheck = (r, ref) -> {
                            BiFunction<Double, LocalRef<BlockState>, Boolean> func = checkMap.get(ref.get().getBlock());
                            if (func != null)
                                return func.apply(r,ref);
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

                    for(RegistryKey<?> key : keys){
                        ProtoSkyMod.baked_masks.put(key, mask);
                    }
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading resource json " + entry.getKey().toString(), t);
            }
        }

        //get the json file in the spawn tree
        List<Resource> spawnResources = manager.getAllResources(SPAWN_IDENTIFIER);

        //parse each json ( should be only one )
        for (Resource resource : spawnResources) {
            try (Reader reader = resource.getReader()) {
                // cast the json to the Config class
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
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading spawn resource json " + SPAWN_IDENTIFIER.toString(), t);
            }
        }

        //get the json file in the world tree
        List<Resource> worldResources = manager.getAllResources(IGNORED_WORLDS_IDENTIFIER);

        //parse each json ( should be only one )
        for (Resource resource : worldResources) {
            try (Reader reader = resource.getReader()) {
                // cast the json to the Config class
                String[] config = ProtoSkyMod.JSON_READER.fromJson(reader, String[].class);

                for (String worldkey : config) {
                    ProtoSkyMod.ignoredWorlds.add(RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(worldkey)));
                }
            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading debug resource json " + IGNORED_WORLDS_IDENTIFIER.toString(), t);
            }
        }

        //get the json file in the debug tree
        List<Resource> debugResources = manager.getAllResources(DEBUG_IDENTIFIER);

        //parse each json ( should be only one )
        for (Resource resource : debugResources) {
            try (Reader reader = resource.getReader()) {
                // cast the json to the Config class
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

                if (config.preventChunkSave != null) {
                    Debug.preventSave = config.preventChunkSave;
                }

            } catch (Throwable t) {
                ProtoSkyMod.LOGGER.error("Error occurred while loading debug resource json " + DEBUG_IDENTIFIER.toString(), t);
            }
        }
    }
}
