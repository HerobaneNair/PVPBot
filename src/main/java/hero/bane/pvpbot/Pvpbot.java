package hero.bane.pvpbot;

import hero.bane.pvpbot.command.DistanceCommand;
import hero.bane.pvpbot.command.PVPBotCommand;
import hero.bane.pvpbot.command.PlayerCommand;
import hero.bane.pvpbot.command.PlayerSpawnCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PVPBot implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PVPBot");

    @Override
    public void onInitialize() {
        PVPBotSettings.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
        {
            PlayerCommand.register(dispatcher, registryAccess);
            PlayerSpawnCommand.register(dispatcher, registryAccess);
            DistanceCommand.register(dispatcher, registryAccess);
            PVPBotCommand.register(dispatcher, registryAccess);
        });
    }
}
