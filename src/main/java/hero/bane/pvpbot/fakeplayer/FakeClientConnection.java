package hero.bane.pvpbot.fakeplayer;

import hero.bane.pvpbot.fakes.ClientConnectionInterface;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Unique;

public class FakeClientConnection extends Connection {
    public FakeClientConnection(PacketFlow p) {
        super(p);
        ((ClientConnectionInterface) this).setChannel(new EmbeddedChannel());
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
    }

}
