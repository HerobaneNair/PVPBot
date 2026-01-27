package hero.bane.pvpbot.util.delayer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public final class DelayedCommandService {

    public static int scheduleCommand(CommandSourceStack source, int ticks, String command) {
        return schedule(source, ticks, command, false);
    }

    public static int scheduleFunction(CommandSourceStack source, int ticks, String functionId) {
        return schedule(source, ticks, functionId, true);
    }

    private static int schedule(CommandSourceStack source, int ticks, String runnables, boolean isFunction) {
        String id = UUID.randomUUID().toString();
        long executeAt = source.getLevel().getGameTime() + ticks;

        UUID executor = source.getEntity() != null
                ? source.getEntity().getUUID()
                : new UUID(0L, 0L);

        DelayedCommandData.add(
                new DelayedCommandData.Entry(id, executor, runnables, isFunction, executeAt)
        );

        source.getServer()
                .getWorldData()
                .overworldData()
                .getScheduledEvents()
                .schedule(id, executeAt, new DelayedCommandData.Callback(id, source, runnables, isFunction));

        return 1;
    }

    public static int list(CommandSourceStack source, UUID filter) {
        ServerLevel level = source.getLevel();
        List<DelayedCommandData.Entry> entries =
                filter == null ? DelayedCommandData.snapshot() : DelayedCommandData.snapshot(filter);

        source.sendSuccess(() -> Component.literal("/delayed queue:\n"), false);
        if(entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(" (Empty)"), false);
        }

        for (int i = 0; i < entries.size(); i++) {
            DelayedCommandData.Entry e = entries.get(i);

            long ticks = e.remainingTicks(level);
            double seconds = ticks / 20.0;

            String delete = "/delayed queue remove " + i;

            Component uuid =
                    Component.literal("UUID: " + e.executor.toString())
                            .withColor(0xCCFFCC);

            Component runnables =
                    Component.literal(e.isFunction ? "\nFunction: " : "\nCommand: ")
                            .withColor(0xFFFFCC)
                            .append(
                                    Component.literal(e.runnables)
                                            .withStyle(style ->
                                                    style.withHoverEvent(
                                                            new HoverEvent.ShowText(
                                                                    Component.literal(e.runnables)
                                                            )
                                                    ).withColor(0xFFFFCC)
                                            )
                            );

            Component delay =
                    Component.literal("\nDelay Remaining: " + ticks + " ticks [" + seconds + "s]");

            Component delayHint =
                    Component.literal("\nClick to copy remove command")
                            .withStyle(style ->
                                    style.withClickEvent(new ClickEvent.SuggestCommand(delete))
                                            .withHoverEvent(
                                                    new HoverEvent.ShowText(
                                                            Component.literal(delete)
                                                    )
                                            )
                                            .withColor(0xFFCCCC)
                            );

            source.sendSuccess(() ->
                            Component.literal("")
                                    .append(uuid)
                                    .append(runnables)
                                    .append(delay)
                                    .append(delayHint)
                                    .append("\n---"),
                    false
            );
        }

        return entries.size();
    }

    public static int remove(CommandSourceStack source, int index) {
        List<DelayedCommandData.Entry> entries = DelayedCommandData.snapshot();

        if (index < 0 || index >= entries.size()) {
            source.sendFailure(
                    Component.literal("Invalid queue index: " + index).withColor(0xFFAAAA)
            );
            return 0;
        }

        DelayedCommandData.Entry removed = DelayedCommandData.remove(index);
        source.getServer()
                .getWorldData()
                .overworldData()
                .getScheduledEvents()
                .remove(removed.id);

        source.sendSuccess(
                () -> Component.literal((removed.isFunction ? ("Removed Func: ") : ("Removed Cmd: ")) + removed.runnables),
                false
        );

        return 1;
    }

    public static int clear(CommandSourceStack source) {
        List<DelayedCommandData.Entry> entries = DelayedCommandData.snapshot();
        source.sendSuccess(() -> Component.literal(entries.size() + " delayed runnables removed"), false);
        for (DelayedCommandData.Entry e : entries) {
            source.getServer()
                    .getWorldData()
                    .overworldData()
                    .getScheduledEvents()
                    .remove(e.id);
        }
        return DelayedCommandData.clearAndReturnCount();
    }
}
