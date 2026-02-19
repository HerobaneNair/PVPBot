package hero.bane.pvpbot.client.mixin;

import hero.bane.pvpbot.PVPBotSettings;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @ModifyConstant(
            method = "aiStep",
            constant = @Constant(floatValue = 3.0F)
    )
    private float changeCreativeVerticalSpeed(float original) {
        return (float) (original * PVPBotSettings.creativeFlySpeed);
    }
}
