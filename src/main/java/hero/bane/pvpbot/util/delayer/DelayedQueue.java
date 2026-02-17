package hero.bane.pvpbot.util.delayer;

import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DelayedQueue {

    public enum ExecutorType {
        PLAYER,
        COMMAND_BLOCK,
        CONSOLE
    }

    public static final class ExecutorData {
        public final ExecutorType type;
        public final UUID player;
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;

        private ExecutorData(ExecutorType type, UUID player,
                             ResourceKey<Level> dimension, BlockPos pos) {
            this.type = type;
            this.player = player;
            this.dimension = dimension;
            this.pos = pos;
        }

        public static ExecutorData player(UUID uuid) {
            return new ExecutorData(ExecutorType.PLAYER, uuid, null, null);
        }

        public static ExecutorData commandBlock(ResourceKey<Level> dimension, BlockPos pos) {
            return new ExecutorData(ExecutorType.COMMAND_BLOCK, null, dimension, pos);
        }

        public static ExecutorData console() {
            return new ExecutorData(ExecutorType.CONSOLE, null, null, null);
        }
    }

    public static final class Entry {
        public final String id;
        public final ExecutorData executor;
        public final String payload;
        public final boolean isFunction;
        public final long executeAt;

        public Entry(String id,
                     ExecutorData executor,
                     String payload,
                     boolean isFunction,
                     long executeAt) {
            this.id = id;
            this.executor = executor;
            this.payload = payload;
            this.isFunction = isFunction;
            this.executeAt = executeAt;
        }

        public long remainingTicks(ServerLevel level) {
            return Math.max(0L, executeAt - level.getGameTime());
        }

        public void execute(MinecraftServer server) {

            CommandSourceStack source;

            if (executor.type == ExecutorType.PLAYER) {

                var player = server.getPlayerList().getPlayer(executor.player);
                if (player == null) return;

                source = player.createCommandSourceStack()
                        .withCallback(CommandResultCallback.EMPTY);

            } else if (executor.type == ExecutorType.COMMAND_BLOCK) {

                ServerLevel level = server.getLevel(executor.dimension);
                if (level == null) return;

                source = new CommandSourceStack(
                        server,
                        executor.pos.getCenter(),
                        Vec2.ZERO,
                        level,
                        PermissionSet.ALL_PERMISSIONS,
                        "@",
                        net.minecraft.network.chat.Component.literal("@"),
                        server,
                        null
                ).withCallback(CommandResultCallback.EMPTY);

            } else {

                source = server.createCommandSourceStack()
                        .withCallback(CommandResultCallback.EMPTY);
            }

            if (isFunction) {
                server.getCommands().performPrefixedCommand(
                        source,
                        "function " + payload
                );
            } else {
                server.getCommands().performPrefixedCommand(
                        source,
                        payload
                );
            }
        }
    }

    private static final List<Entry> QUEUE = new ArrayList<>();

    public static synchronized void add(Entry entry) {
        QUEUE.add(entry);
    }

    public static synchronized List<Entry> snapshot() {
        return List.copyOf(QUEUE);
    }

    public static synchronized List<Entry> snapshotPlayer(UUID uuid) {
        return QUEUE.stream()
                .filter(e -> e.executor.type == ExecutorType.PLAYER
                        && e.executor.player.equals(uuid))
                .toList();
    }

    public static synchronized Entry remove(int index) {
        return QUEUE.remove(index);
    }

    public static synchronized int clearAndReturnCount() {
        int size = QUEUE.size();
        QUEUE.clear();
        return size;
    }

    public static void tick(MinecraftServer server) {

        ServerLevel overworld = server.overworld();

        long time = overworld.getGameTime();

        List<Entry> toExecute = new ArrayList<>();

        synchronized (DelayedQueue.class) {
            for (Entry e : QUEUE) {
                if (time >= e.executeAt) {
                    toExecute.add(e);
                }
            }
            QUEUE.removeAll(toExecute);
        }

        for (Entry e : toExecute) {
            e.execute(server);
        }
    }
}
