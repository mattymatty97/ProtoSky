package protosky.datapack.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import protosky.datapack.ResourceReloader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represents the json file of the Graces
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraceConfig {

    public Override override = new Override();

    //the probability of this specific structure to be placed in the world ( this is on top of vanilla rarity )
    public Double probability;

    //the list of blocks that have to survive the pruning
    public Collection<BlockConfig> blocks;

    //the list of entities that have to survive the pruning
    public Collection<EntityConfig> entities;

    /**
     * This class holds the information for graced BlockStates
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlockConfig extends SubConfig {

        //the block id to save
        public String block;

        //the list of state keys to be checked
        public Collection<BlockConfig.StateConfig<?>> states;

        //list of state keys to be forcefully set if this survives the checks
        public Collection<BlockConfig.StateConfig<?>> forced_states;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StateConfig<T extends Comparable<T>> {

            public String key;
            public T value;

        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EntityConfig extends SubConfig {

        //the entity id to save
        public String entity;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubConfig {
        //the probability of this specific blokc/entity to be placed in the world
        public Double probability;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Override {
        public String name;
        public String namespace;
        public String path;
        public String registry_namespace;
    }


    @JsonIgnore
    private final Map<String, Object> unknown = new HashMap<>();

    // getters/setters omitted

    @JsonAnySetter
    public void set(String name, Object value) {
        unknown.put(name, value);
    }

    public Map<String, Object> getUnknown() {
        return getUnknown(true);
    }

    public Map<String, Object> getUnknown(boolean comments) {
        if (comments)
            return Collections.unmodifiableMap(unknown);
        return unknown.entrySet().stream().filter(e -> !e.getKey().startsWith("_")).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
