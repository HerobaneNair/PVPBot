package hero.bane.pvpbot.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.pvpbot.PVPBot;
import hero.bane.pvpbot.rule.RuleCommandBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public final class PVPBotCommand {
    private static final String versionProblems = "Version Getter Messed Up, ping HerobaneNair or fix fabric.mod";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                literal("pvpbot")
                        .executes(PVPBotCommand::version)
                        .then(RuleCommandBuilder.build())
        );
    }

    private static int version(CommandContext<CommandSourceStack> context) {
        String pvpBotVersion = FabricLoader.getInstance()
                .getModContainer("pvpbot")
                .get().getMetadata().getVersion().getFriendlyString();

        int versionReturned = 0;

        try {
            versionReturned = Integer.parseInt(pvpBotVersion.substring(pvpBotVersion.indexOf('-') + 1, pvpBotVersion.indexOf('+')).replaceAll("\\.", ""));
        } catch (Exception e) {
            context.getSource().sendSuccess(() -> Component.literal(versionProblems).withColor(0xFF5555), false);
            PVPBot.LOGGER.error(versionProblems, e);
        }

        context.getSource().sendSuccess(() -> Component.literal("PVPBotVersion: " + pvpBotVersion), false);
        int finalVersionReturned = versionReturned;
        context.getSource().sendSuccess(() -> Component.literal("Returns: " + finalVersionReturned).withColor(0xAAAAAA), false);
        return 1;
    }
}
