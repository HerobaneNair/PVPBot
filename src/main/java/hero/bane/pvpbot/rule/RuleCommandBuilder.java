package hero.bane.pvpbot.rule;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import hero.bane.pvpbot.PVPBotSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class RuleCommandBuilder {

    public static ArgumentBuilder<CommandSourceStack, ?> build() {
        return argument("rule", StringArgumentType.word())
                .suggests((c, b) -> {
                    for (String name : RuleRegistry.all().keySet()) {
                        b.suggest(name);
                    }
                    return b.buildFuture();
                })
                .executes(c -> {
                    RuleEntry rule = RuleRegistry.get(
                            StringArgumentType.getString(c, "rule")
                    );
                    if (rule == null) return 0;

                    c.getSource().sendSuccess(
                            () -> Component.literal(rule.name + " = " + rule.get() + "\n")
                                    .append(Component.literal(rule.description)
                                            .withStyle(s -> s.withColor(TextColor.fromRgb(0xFFFFAA)))),
                            false
                    );
                    return 1;
                })
                .then(buildValueNode());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildValueNode() {
        return argument("value", StringArgumentType.word())
                .suggests((c, b) -> {
                    RuleEntry rule = RuleRegistry.get(
                            StringArgumentType.getString(c, "rule")
                    );
                    if (rule == null) return b.buildFuture();

                    if (rule.type == boolean.class) {
                        b.suggest("true");
                        b.suggest("false");
                    } else if (rule.type.isEnum()) {
                        for (String s : enumNames(rule.type)) {
                            b.suggest(s);
                        }
                    }

                    return b.buildFuture();
                })
                .executes(c -> {
                    RuleEntry rule = RuleRegistry.get(
                            StringArgumentType.getString(c, "rule")
                    );
                    if (rule == null) return 0;

                    Object value = parseValue(
                            rule,
                            StringArgumentType.getString(c, "value")
                    );

                    rule.set(value);
                    reply(c.getSource(), rule, value, false);
                    return 1;
                })
                .then(literal("perm").executes(c -> {
                    RuleEntry rule = RuleRegistry.get(
                            StringArgumentType.getString(c, "rule")
                    );
                    if (rule == null) return 0;

                    Object value = parseValue(
                            rule,
                            StringArgumentType.getString(c, "value")
                    );

                    rule.set(value);
                    RuleConfigIO.save(PVPBotSettings.CONFIG_FILE);
                    reply(c.getSource(), rule, value, true);
                    return 1;
                }));
    }

    private static Object parseValue(RuleEntry rule, String input) {
        if (rule.type == boolean.class) {
            return Boolean.parseBoolean(input);
        }
        if (rule.type == int.class) {
            return Integer.parseInt(input);
        }
        if (rule.type == double.class) {
            double v = Double.parseDouble(input);

            if (rule.name.equals("creativeFlyDrag")) {
                if (v < 0.0 || v > 1.0) {
                    throw new IllegalArgumentException("creativeFlyDrag must be within the range 0-1");
                }
            } else if (rule.name.equals("creativeFlySpeed")) {
                if (v < 0.0) {
                    throw new IllegalArgumentException("creativeFlySpeed must be nonnegative");
                }
            }

            return v;
        }
        if (rule.type.isEnum()) {
            return parseEnum(rule.type, input.toUpperCase());
        }
        throw new IllegalStateException();
    }

    private static void reply(CommandSourceStack src, RuleEntry rule, Object v, boolean perm) {
        src.sendSuccess(
                () -> Component.literal(rule.name + " = " + v)
                        .append(Component.literal(perm ? " [perm]" : " [temp]")
                                .withStyle(s -> s.withColor(TextColor.fromRgb((perm ? 0xAAFFFF : 0xFFFFAA))))),
                false
        );
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T parseEnum(Class<?> type, String value) {
        return Enum.valueOf((Class<T>) type, value);
    }

    private static String[] enumNames(Class<?> type) {
        Object[] constants = type.getEnumConstants();
        String[] out = new String[constants.length];
        for (int i = 0; i < constants.length; i++) {
            out[i] = ((Enum<?>) constants[i]).name().toLowerCase();
        }
        return out;
    }
}
