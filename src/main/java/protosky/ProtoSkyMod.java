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
        public boolean canPlace(LocalRef<BlockState> blockStateRef, Random random, Map<GraceConfig.BlockConfig,AtomicInteger> map) {
            return false;
        }

        @Override
        public boolean canSpawn(LocalRef<Entity> entityRef, Random random) {
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
            baked_masks.clear();
            Map<Identifier, Resource> resourceMap = manager.findResources(getFabricId().getPath(), id -> id.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()){
                try(Reader reader = entry.getValue().getReader()) {
                    // Consume the stream however you want, medium, rare, or well done.
                    GraceConfig config = JSON_READER.fromJson(reader, GraceConfig.class);

                    String name = config.override_name;

                    if (name == null || name.isEmpty() || name.isBlank()){
                        String currPath = entry.getKey().getPath();
                        String main_path = currPath.substring(IDENTIFIER.getPath().length());
                        if (main_path.startsWith("/"))
                            main_path = main_path.substring(1);
                        String[] sections = main_path.split("/");
                        if (sections.length > 1){
                            String namespace = sections[0];
                            String resourceName = sections[sections.length - 1].replace(".json", "");
                            List<String> subSections = Arrays.stream(sections).skip(1).limit(sections.length - 2).toList();
                            Identifier registry = new Identifier(namespace, Strings.join(subSections, "/"));
                            Identifier resource = new Identifier(namespace, resourceName);
                            RegistryKey<?> key = RegistryKey.of(RegistryKey.ofRegistry(registry),resource);
                            name = key.toString();
                        }
                    }

                    //do not bother if it was already set
                    if (!ProtoSkyMod.baked_masks.containsKey(name)) {

                        //bake the checks
                        Function<Random, Boolean> mainCheck = getProbabilityCheck(config.probability);

                        BiFunction<Random, LocalRef<Entity>, Boolean> entityCheck;

                        if (config.entities != null) {
                            BiFunction<Random, LocalRef<Entity>, Boolean> tmpEntityCheck = (r, ref) -> false;
                            for (GraceConfig.EntityConfig entityConfig : config.entities) {
                                BiFunction<Random, LocalRef<Entity>, Boolean> oldEntityCheck = tmpEntityCheck;
                                BiFunction<Random, LocalRef<Entity>, Boolean> currEntityCheck = makeEntityCheck(entityConfig);
                                tmpEntityCheck = (r, ref) -> oldEntityCheck.apply(r, ref) || currEntityCheck.apply(r, ref);
                            }
                            entityCheck = tmpEntityCheck;
                        } else {
                            entityCheck = (r, ref) -> true;
                        }

                        TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> blockCheck;

                        if (config.blocks != null) {
                            TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> tmpBlockCheck = (r, ref, map) -> false;
                            for (GraceConfig.BlockConfig blockConfig : config.blocks) {
                                TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> oldBlockCheck = tmpBlockCheck;
                                TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> currBlockCheck = makeBlockCheck(blockConfig);
                                tmpBlockCheck = (r, ref, map) -> oldBlockCheck.apply(r, ref, map) || currBlockCheck.apply(r, ref, map);
                            }
                            blockCheck = tmpBlockCheck;
                        } else {
                            blockCheck = (r, ref, map) -> true;
                        }

                        FeatureWorldMask mask = new FeatureWorldMask() {
                            @Override
                            public boolean hasGraces(Random random) {
                                return mainCheck.apply(random);
                            }

                            @Override
                            public boolean canPlace(LocalRef<BlockState> blockState, Random random, Map<GraceConfig.BlockConfig,AtomicInteger> map) {
                                return blockCheck.apply(random, blockState, map);
                            }

                            @Override
                            public boolean canSpawn(LocalRef<Entity> entity, Random random) {
                                return entityCheck.apply(random, entity);
                            }
                        };

                        ProtoSkyMod.baked_masks.put(name, mask);
                    }
                } catch(Throwable t) {
                    LOGGER.error("Error occurred while loading resource json " + entry.getKey().toString(), t);
                }
            }
        }

        @NotNull
        private static BiFunction<Random, LocalRef<Entity>, Boolean> makeEntityCheck(GraceConfig.EntityConfig entityConfig) {
            BiFunction<Random, LocalRef<Entity>, Boolean> currEntityCheck;

            Function<String,Boolean> nameCheck;
            if (entityConfig.entity != null)
                nameCheck = name -> name.equals(entityConfig.entity);
            else {
                nameCheck = null;
            }

            Function<Random, Boolean> probabilityCheck = getProbabilityCheck(entityConfig.probability);

            Supplier<Boolean> count_check;
            if (entityConfig.max_count != null && entityConfig.max_count >= 0){
                AtomicInteger counter = new AtomicInteger();
                count_check = () -> counter.getAndIncrement() < entityConfig.max_count;
            }else{
                count_check = null;
            }

            currEntityCheck = (r, ref) ->
                    ( (nameCheck!=null) ? nameCheck.apply(Registries.ENTITY_TYPE.getId(ref.get().getType()).toString()) : true ) &&
                    ( (probabilityCheck!=null) ? probabilityCheck.apply(r) : true ) &&
                    ( (count_check!=null) ? count_check.get() : true );
            return currEntityCheck;
        }
        @NotNull
        private static TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> makeBlockCheck(GraceConfig.BlockConfig blockConfig) {

            TriFunction<Random, LocalRef<BlockState>, Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> currBlockCheck;

            Function<String,Boolean> nameCheck;
            if (blockConfig.block != null)
                nameCheck = name -> name.equals(blockConfig.block);
            else {
                nameCheck = null;
            }



            Function<Random, Boolean> probabilityCheck = getProbabilityCheck(blockConfig.probability);



            Function<Map<GraceConfig.BlockConfig,AtomicInteger>, Boolean> count_check;
            if (blockConfig.max_count != null && blockConfig.max_count >= 0){
                count_check = (map) -> map.computeIfAbsent(blockConfig, b ->new AtomicInteger() ).getAndIncrement() < blockConfig.max_count;
            }else{
                count_check = null;
            }



            Function<BlockState,Boolean> stateCheck;
            if (blockConfig.states != null){

                Function<BlockState,Boolean> tmpStateCheck = s -> true;
                for (GraceConfig.BlockConfig.StateConfig stateConfig : blockConfig.states){
                    Function<BlockState,Boolean> currStateCheck = tmpStateCheck;
                    tmpStateCheck = blockState -> {
                        if (currStateCheck.apply(blockState)) {
                            Collection<Property<?>> properties = blockState.getProperties();
                            Optional<Property<?>> optionalProperty = properties.stream().filter(p -> p.getName().equals(stateConfig.key)).findAny();
                            if (optionalProperty.isPresent()) {
                                return blockState.get(optionalProperty.get()).equals(stateConfig.value);
                            }
                        }
                        return false;
                    };
                }
                stateCheck = tmpStateCheck;
            }else{
                stateCheck = null;
            }

            /*
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

        @Nullable
        private static Function<Random, Boolean> getProbabilityCheck(Double blockConfig) {
            Function<Random, Boolean> probabilityCheck;
            if (blockConfig != null) {
                if (blockConfig <= 1.0 && blockConfig >= 0.0)
                    probabilityCheck = random -> random.nextFloat() < blockConfig;
                else if (blockConfig <= 0.0)
                    probabilityCheck = random -> false;
                else
                    probabilityCheck = null;
            } else {
                probabilityCheck = null;
            }
            return probabilityCheck;
        }
    }

    public static class GraceConfig{

        protected String override_name;

        protected Double probability;

        protected Collection<BlockConfig> blocks;

        protected Collection<EntityConfig> entities;

        public static class BlockConfig{

            protected String block;

            protected Double probability;
            protected Integer max_count;
            protected Collection<StateConfig<?>> states;
            protected Collection<StateConfig<?>> forced_states;

            public static class StateConfig<T extends Comparable<T>>{

                protected String key;
                protected T value;

            }
        }

        public static class EntityConfig{

            protected String entity;

            protected Double probability;
            protected Integer max_count;
        }

    }
}
