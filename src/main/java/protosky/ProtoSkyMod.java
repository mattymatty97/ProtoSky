package protosky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import joptsimple.internal.Strings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import protosky.interfaces.FeatureWorldMask;

import java.io.Reader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked","rawtypes"})
public class ProtoSkyMod implements ModInitializer {

    private static final Runnable NOOP = () -> {};

    private static final Identifier MOD_IDENTIFIER = new Identifier("protosky");

    public static final String GRACES_TAG = "protosky_graces";
    public static final String OLD_STATUS_TAG = "protosky_old_status";
    public static final ResourceBundle PLACEHOLDERS;
    public static final Logger LOGGER = LogManager.getLogger("ProtoSky");

    public static final Map<String, FeatureWorldMask> baked_masks = new LinkedHashMap<>();
    @Unique
    public static final FeatureWorldMask EMPTY_MASK = new FeatureWorldMask() {
        @Override
        public boolean hasGraces(Random random) {
            return false;
        }

        @Override
        public boolean canPlace(LocalRef<BlockState> blockStateRef, Random random, Map<GraceConfig.SubConfig,AtomicInteger> map) {
            return false;
        }

        @Override
        public boolean canSpawn(LocalRef<Entity> entityRef, Random random, Map<GraceConfig.SubConfig,AtomicInteger> map) {
            return false;
        }
    };

    static {
        PLACEHOLDERS = ResourceBundle.getBundle("placeholders");
    }
    @Override
    public void onInitialize() {
        LOGGER.info("Protosky %s build %s loaded, have a void day!".formatted(PLACEHOLDERS.getString("version"), PLACEHOLDERS.getString("build")));
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ResourceReloadListener());
    }


    public static final Gson JSON_READER = new GsonBuilder().setLenient().disableHtmlEscaping().create();

    private static class ResourceReloadListener implements SimpleSynchronousResourceReloadListener{

        public static final Identifier IDENTIFIER = MOD_IDENTIFIER.withPath("grace");
        @Override
        public Identifier getFabricId() {
            return IDENTIFIER;
        }

        @Override
        public void reload(ResourceManager manager) {
            //forget old data
            baked_masks.clear();

            //get all json files in the grace tree
            Map<Identifier, Resource> resourceMap = manager.findResources(getFabricId().getPath(), id -> id.getPath().endsWith(".json"));

            //parse each json
            for (Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()){
                try(Reader reader = entry.getValue().getReader()) {
                    // cast the json to the Config class
                    GraceConfig config = JSON_READER.fromJson(reader, GraceConfig.class);

                    //obtain this config name/key
                    String name = config.override_name;
                    if (name == null || name.isEmpty() || name.isBlank()){
                        //generate the name based on the path the file was
                        String currPath = entry.getKey().getPath();
                        //remove the prefix from the path
                        String main_path = currPath.substring(IDENTIFIER.getPath().length());
                        //remove extra / if present
                        if (main_path.startsWith("/"))
                            main_path = main_path.substring(1);
                        //obtain the tree structure
                        String[] sections = main_path.split("/");
                        if (sections.length > 1){
                            //first folder is the vanilla namespace
                            String namespace = sections[0];
                            //last section is the filename, we trim it from the extension to get the vanilla resource name
                            String resourceName = sections[sections.length - 1].replace(".json", "");
                            //generate the vanilla resource path from the remaining parts
                            List<String> subSections = Arrays.stream(sections).skip(1).limit(sections.length - 2).toList();
                            Identifier registry = new Identifier(namespace, Strings.join(subSections, "/"));
                            Identifier resource = new Identifier(namespace, resourceName);
                            RegistryKey<?> key = RegistryKey.of(RegistryKey.ofRegistry(registry),resource);
                            name = key.toString();
                        }
                    }

                    //do not bother if it was already set
                    if (!ProtoSkyMod.baked_masks.containsKey(name)) {

                        //bake the probability check
                        Function<Random, Boolean> mainCheck = getProbabilityCheck(config.probability);

                        //if there is an entity section iterate over it
                        TriFunction<Random, LocalRef<Entity>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> entityCheck;
                        if (config.entities != null) {
                            //start with an always false check to fail in case of empty list
                            TriFunction<Random, LocalRef<Entity>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> tmpEntityCheck = (r, ref, map) -> false;
                            for (GraceConfig.EntityConfig entityConfig : config.entities) {
                                TriFunction<Random, LocalRef<Entity>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> oldEntityCheck = tmpEntityCheck;
                                TriFunction<Random, LocalRef<Entity>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> currEntityCheck = makeEntityCheck(entityConfig);
                                //append the new check in or with the previous ones
                                tmpEntityCheck = (r, ref, map) -> oldEntityCheck.apply(r, ref, map) || currEntityCheck.apply(r, ref, map);
                            }
                            entityCheck = tmpEntityCheck;
                        } else {
                            entityCheck = (r, ref, map) -> true;
                        }

                        //if there is a block check
                        TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> blockCheck;
                        if (config.blocks != null) {
                            //start with an always false check to fail in case of empty list
                            TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> tmpBlockCheck = (r, ref, map) -> false;
                            for (GraceConfig.BlockConfig blockConfig : config.blocks) {
                                TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> oldBlockCheck = tmpBlockCheck;
                                TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> currBlockCheck = makeBlockCheck(blockConfig);
                                //append the new check in or with the previous ones
                                tmpBlockCheck = (r, ref, map) -> oldBlockCheck.apply(r, ref, map) || currBlockCheck.apply(r, ref, map);
                            }
                            blockCheck = tmpBlockCheck;
                        } else {
                            blockCheck = (r, ref, map) -> true;
                        }

                        //combine all the backed checks into the Mask Class
                        FeatureWorldMask mask = new FeatureWorldMask() {
                            @Override
                            public boolean hasGraces(Random random) {
                                return mainCheck.apply(random);
                            }

                            @Override
                            public boolean canPlace(LocalRef<BlockState> blockState, Random random, Map<GraceConfig.SubConfig,AtomicInteger> map) {
                                return blockCheck.apply(random, blockState, map);
                            }

                            @Override
                            public boolean canSpawn(LocalRef<Entity> entity, Random random, Map<GraceConfig.SubConfig,AtomicInteger> map) {
                                return entityCheck.apply(random, entity, map);
                            }
                        };

                        //and add the mask to the map
                        ProtoSkyMod.baked_masks.put(name, mask);
                    }
                } catch(Throwable t) {
                    LOGGER.error("Error occurred while loading resource json " + entry.getKey().toString(), t);
                }
            }
        }

        /**
         * Bake the check for the specified entity Object
         * @param entityConfig the config for the current check
         * @return the baked function to be checked against
         */
        @NotNull
        private static TriFunction<Random, LocalRef<Entity>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> makeEntityCheck(GraceConfig.EntityConfig entityConfig) {
            TriFunction<Random, LocalRef<Entity>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> currEntityCheck;

            //if it has a name check
            Function<String,Boolean> nameCheck;
            if (entityConfig.entity != null)
                //check if the entity id matches the one in the config
                nameCheck = name -> name.equals(entityConfig.entity);
            else {
                nameCheck = null;
            }

            //generate the probability check
            Function<Random, Boolean> probabilityCheck = getProbabilityCheck(entityConfig.probability);

            //if it has a max_count check
            Function<Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> count_check;
            if (entityConfig.max_count != null && entityConfig.max_count >= 0){
                //obtain the counter from the map and increment it
                count_check = (map) -> map.computeIfAbsent(entityConfig, k -> new AtomicInteger()).getAndIncrement() < entityConfig.max_count;
            }else{
                count_check = null;
            }

            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currEntityCheck = (r, ref, map) ->
                    ( (nameCheck!=null) ? nameCheck.apply(Registries.ENTITY_TYPE.getId(ref.get().getType()).toString()) : true ) &&
                    ( (probabilityCheck!=null) ? probabilityCheck.apply(r) : true ) &&
                    ( (count_check!=null) ? count_check.apply(map) : true );
            return currEntityCheck;
        }


        /**
         * Bake the check for the specified BlockState Object
         * @param blockConfig the config for the current check
         * @return the baked function to be checked against
         */
        @NotNull
        private static TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.SubConfig,AtomicInteger>, Boolean> makeBlockCheck(GraceConfig.BlockConfig blockConfig) {

            TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.SubConfig, AtomicInteger>, Boolean> currBlockCheck;

            //if it has a name check
            Function<String,Boolean> nameCheck;
            if (blockConfig.block != null)
                //check if the block id matches the one in the config
                nameCheck = name -> name.equals(blockConfig.block);
            else {
                nameCheck = null;
            }


            //generate the probability check
            Function<Random, Boolean> probabilityCheck = getProbabilityCheck(blockConfig.probability);


            //if it has a max_count check
            Function<Map<GraceConfig.SubConfig, AtomicInteger>, Boolean> count_check;
            if (blockConfig.max_count != null && blockConfig.max_count >= 0){
                //obtain the counter from the map and increment it
                count_check = (map) -> map.computeIfAbsent(blockConfig, b ->new AtomicInteger() ).getAndIncrement() < blockConfig.max_count;
            }else{
                count_check = null;
            }

            //if there are blockstates to be checked
            Function<BlockState,Boolean> stateCheck;
            if (blockConfig.states != null){
                //start with true if no blockstates have to be checked
                Function<BlockState,Boolean> tmpStateCheck = s -> true;
                //for each blockstate
                for (GraceConfig.BlockConfig.StateConfig stateConfig : blockConfig.states){
                    Function<BlockState,Boolean> currStateCheck = tmpStateCheck;
                    //check if the blockstate beeing placed has the same value as the Config
                    Function<BlockState,Boolean> localStateCheck = blockState -> {
                        Collection<Property<?>> properties = blockState.getProperties();
                        Optional<Property<?>> optionalProperty = properties.stream().filter(p -> p.getName().equals(stateConfig.key)).findAny();
                        return optionalProperty.filter(property -> blockState.get(property).equals(stateConfig.value)).isPresent();
                    };
                    //combine the checks in AND with each other
                    tmpStateCheck = blockState -> currStateCheck.apply(blockState) && localStateCheck.apply(blockState);
                }
                stateCheck = tmpStateCheck;
            }else{
                stateCheck = null;
            }

            /* //the code for forcing a blockstate is commented because I was not able to make the castings work as intended
            Consumer<LocalRef<BlockState>> blockStateProcessor;
            if (blockConfig.forced_states != null){
                Consumer<LocalRef<BlockState>> tmpBlockStateProcessor = ref -> {
                    BlockState state = ref.get();
                    ref.set(new BlockState(state.getBlock(),state.getEntries(), ((StateAccessor<BlockState>)state).getCodec()));
                };
                for (GraceConfig.BlockConfig.StateConfig stateConfig : blockConfig.forced_states){
                    Consumer<LocalRef<BlockState>> currBlockStateProcessor = tmpBlockStateProcessor;
                    tmpBlockStateProcessor = ref -> {
                        currBlockStateProcessor.accept(ref);

                        Collection<Property<?>> properties = ref.get().getProperties();
                        Optional<Property<?>> optionalProperty = properties.stream().filter(p -> p.getName().equals(stateConfig.key)).findAny();
                        if (optionalProperty.isPresent()) {
                            Class<?> propertyClass = optionalProperty.get().getType();
                            if (stateConfig.value.getClass().isAssignableFrom(propertyClass))
                                ref.get().with(optionalProperty.get(), stateConfig.value);
                        }
                    };
                }
                blockStateProcessor = tmpBlockStateProcessor;
            }else{
                blockStateProcessor = null;
            }
            */

            //combine all the checks in AND with each other, have the count check last, so it will only trigger when all the other checks are positive
            currBlockCheck = (r, ref, map) -> {
                        boolean result =
                        ( (nameCheck!=null) ? nameCheck.apply(Registries.BLOCK.getId(ref.get().getBlock()).toString()) : true ) &&
                        ( (stateCheck!=null) ? stateCheck.apply(ref.get()) : true ) &&
                        ( (probabilityCheck!=null) ? probabilityCheck.apply(r) : true ) &&
                        ( (count_check!=null) ? count_check.apply(map) : true );
                        /*if (result && blockStateProcessor != null)
                            blockStateProcessor.accept(ref);*/
                        return result;
            };
            return currBlockCheck;
        }


        /**
         * prepare the probability check given the Double value
         * @param probability the probability to be checked against. range -> [0.0, 1.0]
         * @return the probability check
         */
        private static Function<Random, Boolean> getProbabilityCheck(Double probability) {
            Function<Random, Boolean> probabilityCheck;
            if (probability != null) {
                if (probability <= 1.0 && probability >= 0.0)
                    probabilityCheck = random -> random.nextFloat() < probability;
                else if (probability <= 0.0)
                    probabilityCheck = random -> false;
                else
                    probabilityCheck = r -> true;
            } else {
                probabilityCheck = r -> true;
            }
            return probabilityCheck;
        }
    }

    /**
     * This class represents the json file of the Graces
     */
    public static class GraceConfig{

        //forced map key to be used in case of modded structures/features
        protected String override_name;

        //the probability of this specific structure to be placed in the world ( this is on top of vanilla rarity )
        protected Double probability;

        //the list of blocks that have to survive the pruning
        protected Collection<BlockConfig> blocks;

        //the list of entities that have to survive the pruning
        protected Collection<EntityConfig> entities;

        /**
         * This class holds the information for graced BlockStates
         */
        public static class BlockConfig extends SubConfig{

            //the block id to save
            protected String block;

            //the list of state keys to be checked
            protected Collection<StateConfig<?>> states;

            //list of state keys to be forcefully set if this survives the checks
            protected Collection<StateConfig<?>> forced_states;

            public static class StateConfig<T extends Comparable<T>>{

                protected String key;
                protected T value;

            }
        }

        public static class EntityConfig extends SubConfig{

            //the entity id to save
            protected String entity;

        }

        public static class SubConfig{
            //the probability of this specific blokc/entity to be placed in the world
            protected Double probability;

            //the maximum amount of blocks/entities matching this rule to be placed in the world
            //( this count is valid only for a 3x3 chunk area, structures larger than might not conform to this rule )
            protected Integer max_count;
        }

    }
}
