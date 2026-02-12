package hero.bane.pvpbot.util.delayer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public final class DelayedManager {

    public static int scheduleCommand(CommandSourceStack source, int ticks, String command) {
        return schedule(source, ticks, command, false);
    }

    public static int scheduleFunction(CommandSourceStack source, int ticks, String functionId) {
        return schedule(source, ticks, functionId, true);
    }

    private static int schedule(CommandSourceStack source, int ticks, String payload, boolean isFunction) {

        String id = UUID.randomUUID().toString();
        long executeAt = source.getLevel().getGameTime() + ticks;

        UUID executor = source.getEntity() != null
                ? source.getEntity().getUUID()
                : new UUID(0L, 0L);

        DelayedQueue.add(
                new DelayedQueue.Entry(id, executor, payload, isFunction, executeAt)
        );

        source.getServer()
                .getWorldData()
                .overworldData()
                .getScheduledEvents()
                .schedule(id, executeAt, new DelayedQueue.Callback(id, executor, payload, isFunction));

        return 1;
    }

    public static int list(CommandSourceStack source, UUID filter) {
        ServerLevel level = source.getLevel();
        List<DelayedQueue.Entry> entries =
                filter == null ? DelayedQueue.snapshot() : DelayedQueue.snapshot(filter);

        source.sendSuccess(() -> Component.literal("/delayed queue:\n" + (entries.isEmpty() ? " (Empty)" : "")), false);

        for (int i = 0; i < entries.size(); i++) {

            DelayedQueue.Entry e = entries.get(i);

            long ticks = e.remainingTicks(level);
            double seconds = ticks / 20.0;
            String delete = "/delayed queue remove " + i;

            source.sendSuccess(() ->
                            Component.literal("")
                                    .append(Component.literal("UUID: " + e.executor))
                                    .append(Component.literal("\nPayload: " + e.payload))
                                    .append(Component.literal("\nRemaining: " + ticks + " ticks [" + seconds + "s]"))
                                    .append(Component.literal("\nClick to copy remove")
                                            .withStyle(style ->
                                                    style.withClickEvent(new ClickEvent.SuggestCommand(delete))
                                                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(delete)))
                                            )
                                    )
                                    .append("\n"),
                    false
            );
        }

        return entries.size();
    }

    public static int remove(CommandSourceStack source, int index) {

        List<DelayedQueue.Entry> entries = DelayedQueue.snapshot();

        if (index < 0 || index >= entries.size()) {
            source.sendFailure(Component.literal("Invalid queue index: " + index));
            return 0;
        }

        DelayedQueue.Entry removed = DelayedQueue.remove(index);

        source.getServer()
                .getWorldData()
                .overworldData()
                .getScheduledEvents()
                .remove(removed.id);

        source.sendSuccess(() -> Component.literal("Removed: " + removed.payload), false);

        return 1;
    }

    public static int clear(CommandSourceStack source) {

        List<DelayedQueue.Entry> entries = DelayedQueue.snapshot();

        for (DelayedQueue.Entry e : entries) {
            source.getServer()
                    .getWorldData()
                    .overworldData()
                    .getScheduledEvents()
                    .remove(e.id);
        }

        int count = DelayedQueue.clearAndReturnCount();
        source.sendSuccess(() -> Component.literal(count + " delayed payloads removed"), false);

        return count;
    }
}
