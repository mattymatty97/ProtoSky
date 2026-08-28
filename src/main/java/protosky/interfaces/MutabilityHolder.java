package protosky.interfaces;

public interface MutabilityHolder {
    default boolean protoSky$isMutable() { return true; }
}
