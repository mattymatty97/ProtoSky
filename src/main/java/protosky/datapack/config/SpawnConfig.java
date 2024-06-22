package protosky.datapack.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpawnConfig {
    public String worldKey;
    public IntArrayList spawnPos;

    @JsonIgnore
    private final Map<String, Object> unknown = new HashMap<>();

    // getters/setters omitted

    @JsonAnySetter
    public void set(String name, Object value) {
        unknown.put(name, value);
    }

    public Map<String, Object> getUnknown(boolean comments) {
        if (comments)
            return Collections.unmodifiableMap(unknown);
        return unknown.entrySet().stream().filter(e -> !e.getKey().startsWith("_")).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
