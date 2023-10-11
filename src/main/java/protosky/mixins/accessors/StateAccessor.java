package protosky.mixins.accessors;

import com.mojang.serialization.MapCodec;
import net.minecraft.state.State;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(State.class)
public interface StateAccessor<T> {

    @Accessor
    MapCodec<T> getCodec();
}
