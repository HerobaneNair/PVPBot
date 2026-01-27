package hero.bane.pvpbot.util.delayer;

import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.timers.TimerCallback;
import net.minecraft.world.level.timers.TimerQueue;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DelayedCommandData {

    public static final class Entry {
        public final String id;
        public final UUID executor;
        public final String payload;
        public final boolean isFunction;
        public final long executeAt;

        public Entry(String id, UUID executor, String payload, boolean isFunction, long executeAt) {
            this.id = id;
            this.executor = executor;
            this.payload = payload;
            this.isFunction = isFunction;
            this.executeAt = executeAt;
        }

        public long remainingTicks(ServerLevel level) {
            return Math.max(0L, executeAt - level.getGameTime());
        }
    }

    private static final List<Entry> QUEUE = new ArrayList<>();

    public static synchronized void add(Entry entry) {
        QUEUE.add(entry);
    }

    public static synchronized List<Entry> snapshot() {
        return List.copyOf(QUEUE);
    }

    public static synchronized List<Entry> snapshot(UUID executor) {
        return QUEUE.stream().filter(e -> e.executor.equals(executor)).toList();
    }

    public static synchronized Entry remove(int index) {
        return QUEUE.remove(index);
    }

    public static synchronized void removeById(String id) {
        QUEUE.removeIf(e -> e.id.equals(id));
    }

    public static synchronized int clearAndReturnCount() {
        int size = QUEUE.size();
        QUEUE.clear();
        return size;
    }

    public static final class Callback implements TimerCallback<MinecraftServer> {

        public static final MapCodec<Callback> CODEC = MapCodec.unit(() -> null);

        private final String id;
        private final CommandSourceStack source;
        private final String runnableThing;
        private final boolean isFunction;

        public Callback(String id, CommandSourceStack source, String runnableThing, boolean isFunction) {
            this.id = id;
            this.source = source.withCallback(CommandResultCallback.EMPTY);
            this.runnableThing = runnableThing;
            this.isFunction = isFunction;
        }

        @Override
        public void handle(MinecraftServer server, @NonNull TimerQueue<MinecraftServer> queue, long time) {
            try {
                if (source.getEntity() == null) {
                    return;
                }

                if (isFunction) {
                    server.getCommands().performPrefixedCommand(
                            source,
                            "function " + payload
                    );
                } else {
                    server.getCommands().performPrefixedCommand(
                            source,
                            runnableThing
                    );
                }
            } finally {
                DelayedCommandData.removeById(id);
            }
        }

        @Override
        public @NonNull MapCodec<? extends TimerCallback<MinecraftServer>> codec() {
            return CODEC;
        }
    }
}
