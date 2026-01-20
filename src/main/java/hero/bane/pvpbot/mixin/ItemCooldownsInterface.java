package hero.bane.pvpbot.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public interface ItemCooldownsInterface {

    @Accessor("cooldowns")
    Map<ResourceLocation, ?> getCooldowns();

    @Accessor("tickCount")
    int getTickCount();
}