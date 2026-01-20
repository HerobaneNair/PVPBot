package hero.bane.pvpbot.mixin;

import com.mojang.authlib.GameProfile;
import hero.bane.pvpbot.fakeplayer.EntityPlayerMPFake;
import hero.bane.pvpbot.fakeplayer.NetHandlerPlayServerFake;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow @Final
    private MinecraftServer server;

    @Inject(
            method = "load",
            at = @At(value = "RETURN", shift = At.Shift.BEFORE)
    )
    private void fixStartingPos(ServerPlayer player, CallbackInfoReturnable<CompoundTag> cir) {
        if (player instanceof EntityPlayerMPFake fake) {
            fake.fixStartingPosition.run();
        }
    }

    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"
            )
    )
    private ServerGamePacketListenerImpl replaceNetworkHandler(
            MinecraftServer server,
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie
    ) {
        if (player instanceof EntityPlayerMPFake fake) {
            return new NetHandlerPlayServerFake(this.server, connection, fake, cookie);
        }
        return new ServerGamePacketListenerImpl(this.server, connection, player, cookie);
    }

    @Redirect(
            method = "respawn",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"
            )
    )
    private ServerPlayer makePlayerForRespawn(
            MinecraftServer server,
            ServerLevel level,
            GameProfile profile,
            ClientInformation cli,
            ServerPlayer oldPlayer,
            boolean alive
    ) {
        if (oldPlayer instanceof EntityPlayerMPFake) {
            return EntityPlayerMPFake.respawnFake(this.server, level, profile, cli);
        }
        return new ServerPlayer(this.server, level, profile, cli);
    }
}
