package hero.bane.pvpbot.util.delayer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
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

        DelayedQueue.ExecutorData executorData;

        if (source.getEntity() != null) {
            executorData = DelayedQueue.ExecutorData.player(source.getEntity().getUUID());
        } else if (source.getLevel() instanceof ServerLevel level) {
            executorData = DelayedQueue.ExecutorData.commandBlock(
                    level.dimension(),
                    BlockPos.containing(source.getPosition())
            );
        } else {
            executorData = DelayedQueue.ExecutorData.console();
        }

        DelayedQueue.add(
                new DelayedQueue.Entry(id, executorData, payload, isFunction, executeAt)
        );

        return 1;
    }

    public static int list(CommandSourceStack source, UUID filter) {

        ServerLevel level = source.getLevel();
        List<DelayedQueue.Entry> entries =
                filter == null ? DelayedQueue.snapshot()
                        : DelayedQueue.snapshotPlayer(filter);

        source.sendSuccess(() ->
                Component.literal("/delayed queue:\n" + (entries.isEmpty() ? " (Empty)" : "")), false);

        for (int i = 0; i < entries.size(); i++) {

            DelayedQueue.Entry e = entries.get(i);

            long ticks = e.remainingTicks(level);
            double seconds = ticks / 20.0;
            String delete = "/delayed queue remove " + i;

            int finalI = i; //I don't understand but sure
            source.sendSuccess(() ->
                            Component.literal(finalI + ":\n")
                                    .append(Component.literal("Executor: " + e.executor.type).withColor(0xCCFFCC))
                                    .append(Component.literal("\n" + (e.isFunction ? "Function: " : "Command: ") + e.payload).withColor(0xFFFFCC))
                                    .append(Component.literal("\nRemaining: " + ticks + " ticks [" + seconds + "s]").withColor(0xFFE0CC))
                                    .append(Component.literal("\nClick to copy remove").withColor(0xFFCCCC)
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

        source.sendSuccess(() -> Component.literal("Removed: " + removed.payload), false);

        return 1;
    }

    public static int clear(CommandSourceStack source) {

        int count = DelayedQueue.clearAndReturnCount();

        source.sendSuccess(() ->
                Component.literal(count + " delayed payloads removed"), false);

        return count;
    }
}
