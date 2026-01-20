package hero.bane.pvpbot.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import hero.bane.pvpbot.PVPBotSettings;
import hero.bane.pvpbot.util.SimpleMessenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class PVPBotCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx)
    {
        dispatcher.register(
                literal("pvpbot")
                        .requires(s -> s.hasPermission(2))
                        .then(booleanRule(
                                "allowListingFakePlayers",
                                () -> PVPBotSettings.allowListingFakePlayers,
                                v -> PVPBotSettings.allowListingFakePlayers = v,
                                "Allows listing fake players on the multiplayer screen"
                        ))
                        .then(booleanRule(
                                "allowSpawningOfflinePlayers",
                                () -> PVPBotSettings.allowSpawningOfflinePlayers,
                                v -> PVPBotSettings.allowSpawningOfflinePlayers = v,
                                "Spawn offline players in online mode if the specified player does not exist"
                        ))
                        .then(booleanRule("creativeNoClip",
                                () -> PVPBotSettings.creativeNoClip,
                                v -> PVPBotSettings.creativeNoClip = v,
                                "Creative No Clip"))
                        .then(doubleRule("creativeFlySpeed",
                                () -> PVPBotSettings.creativeFlySpeed,
                                v -> PVPBotSettings.creativeFlySpeed = v,
                                v -> v >= 0,
                                "Creative flying speed multiplier"))
                        .then(doubleRule("creativeFlyDrag",
                                () -> PVPBotSettings.creativeFlyDrag,
                                v -> PVPBotSettings.creativeFlyDrag = v,
                                v -> v >= 0 && v <= 1,
                                "Creative air drag"))
                        .then(booleanRule("shieldStunning",
                                () -> PVPBotSettings.shieldStunning,
                                v -> PVPBotSettings.shieldStunning = v,
                                "Enables shield stunning"))
                        .then(booleanRule("editablePlayerNbt",
                                () -> PVPBotSettings.editablePlayerNbt,
                                v -> PVPBotSettings.editablePlayerNbt = v,
                                "Allows editing player NBT"))
                        .then(booleanRule("explosionNoFire",
                                () -> PVPBotSettings.explosionNoFire,
                                v -> PVPBotSettings.explosionNoFire = v,
                                "Intentional explosions do not cause fire"))
                        .then(enumRule("explosionNoBlockDamage",
                                () -> PVPBotSettings.explosionNoBlockDamage,
                                v -> PVPBotSettings.explosionNoBlockDamage = v,
                                PVPBotSettings.ExplosionNoDmgMode.class,
                                "Explosions won't destroy blocks"))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanRule(
            String name,
            java.util.function.Supplier<Boolean> getter,
            java.util.function.Consumer<Boolean> setter,
            String description)
    {
        return literal(name)
                .executes(c -> {
                    SimpleMessenger.send(c.getSource(), description);
                    return 1;
                })
                .then(argument("value", BoolArgumentType.bool())
                        .executes(c -> {
                            boolean v = BoolArgumentType.getBool(c, "value");
                            setter.accept(v);
                            SimpleMessenger.send(c.getSource(), name + ": " + v + " [temporary]");
                            return 1;
                        })
                        .then(literal("perm")
                                .executes(c -> {
                                    boolean v = BoolArgumentType.getBool(c, "value");
                                    setter.accept(v);
                                    SimpleMessenger.send(c.getSource(), name + ": " + v + " [permanently]");
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> doubleRule(
            String name,
            java.util.function.Supplier<Double> getter,
            java.util.function.Consumer<Double> setter,
            java.util.function.Predicate<Double> validator,
            String description)
    {
        return literal(name)
                .executes(c -> {
                    SimpleMessenger.send(c.getSource(), description);
                    return 1;
                })
                .then(argument("value", DoubleArgumentType.doubleArg())
                        .executes(c -> {
                            double v = DoubleArgumentType.getDouble(c, "value");
                            if (!validator.test(v)) return 0;
                            setter.accept(v);
                            SimpleMessenger.send(c.getSource(), name + ": " + v + " [temporary]");
                            return 1;
                        })
                        .then(literal("perm")
                                .executes(c -> {
                                    double v = DoubleArgumentType.getDouble(c, "value");
                                    if (!validator.test(v)) return 0;
                                    setter.accept(v);
                                    SimpleMessenger.send(c.getSource(), name + ": " + v + " [permanently]");
                                    return 1;
                                })));
    }

    private static <E extends Enum<E>> LiteralArgumentBuilder<CommandSourceStack> enumRule(
            String name,
            java.util.function.Supplier<E> getter,
            java.util.function.Consumer<E> setter,
            Class<E> enumClass,
            String description)
    {
        return literal(name)
                .executes(c -> {
                    SimpleMessenger.send(c.getSource(), description);
                    return 1;
                })
                .then(argument("value", StringArgumentType.word())
                        .executes(c -> {
                            try {
                                E v = Enum.valueOf(enumClass, StringArgumentType.getString(c, "value").toUpperCase());
                                setter.accept(v);
                                SimpleMessenger.send(c.getSource(), name + ": " + v + " [temporary]");
                                return 1;
                            } catch (IllegalArgumentException e) {
                                return 0;
                            }
                        })
                        .then(literal("perm")
                                .executes(c -> {
                                    try {
                                        E v = Enum.valueOf(enumClass, StringArgumentType.getString(c, "value").toUpperCase());
                                        setter.accept(v);
                                        SimpleMessenger.send(c.getSource(), name + ": " + v + " [permanently]");
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        return 0;
                                    }
                                })));
    }
}
