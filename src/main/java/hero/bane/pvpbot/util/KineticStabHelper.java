package hero.bane.pvpbot.util;

import hero.bane.pvpbot.mixin.LivingEntityAccessor;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.item.Items;

public final class KineticStabHelper {

    private KineticStabHelper() {}

//    public static void beginStab(ServerPlayer player) {
//        ItemStack stack = player.getMainHandItem();
//        if (!stack.has(DataComponents.KINETIC_WEAPON)) return;
//
//        LivingEntityAccessor accessor = (LivingEntityAccessor) player;
//
//        if (accessor.getRecentKineticEnemies() == null) {
//            accessor.setRecentKineticEnemies(new Object2LongOpenHashMap<Entity>());
//        } else {
//            accessor.getRecentKineticEnemies().clear();
//        }
//
//        KineticWeapon kw = stack.get(DataComponents.KINETIC_WEAPON);
//        if (kw != null) {
//            player.onAttack();
//            player.lungeForwardMaybe();
//            player.startAutoSpinAttack(0);
//            kw.makeSound(player);
//        }
//    }


}
