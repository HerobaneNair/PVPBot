package hero.bane.pvpbot.command;

import com.mojang.brigadier.CommandDispatcher;
import hero.bane.pvpbot.rule.RuleCommandBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public final class PVPBotCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                literal("pvpbot")
                        .then(RuleCommandBuilder.build())
        );
    }
}
