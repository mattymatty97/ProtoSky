package protosky.datapack.config;

import protosky.datapack.ResourceReloader;

import java.util.Collection;

/**
 * This class represents the json file of the Graces
 */
public class GraceConfig {

    //forced map key to be used in case of modded structures/features
    public String override_name;

    //the probability of this specific structure to be placed in the world ( this is on top of vanilla rarity )
    public Double probability;

    //the list of blocks that have to survive the pruning
    public Collection<BlockConfig> blocks;

    //the list of entities that have to survive the pruning
    public Collection<EntityConfig> entities;

    /**
     * This class holds the information for graced BlockStates
     */
    public static class BlockConfig extends SubConfig {

        //the block id to save
        public String block;

        //the list of state keys to be checked
        public Collection<BlockConfig.StateConfig<?>> states;

        //list of state keys to be forcefully set if this survives the checks
        public Collection<BlockConfig.StateConfig<?>> forced_states;

        public static class StateConfig<T extends Comparable<T>> {

            public String key;
            public T value;

        }
    }

    public static class EntityConfig extends SubConfig {

        //the entity id to save
        public String entity;

    }

    public static class SubConfig {
        //the probability of this specific blokc/entity to be placed in the world
        public Double probability;

    }

}
