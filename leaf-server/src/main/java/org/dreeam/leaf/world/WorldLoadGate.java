package org.dreeam.leaf.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.dreeam.leaf.config.modules.async.PrefetchChunks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports whether a world is idle enough to spend cycles on speculative work
 * (see {@link org.dreeam.leaf.async.chunk.ChunkPrefetchScheduler}), based on its
 * 1-minute average MSPT ({@link org.dreeam.leaf.config.modules.async.PrefetchChunks}).
 */
public final class WorldLoadGate {

    private WorldLoadGate() {
    }

    private static final Map<ServerLevel, CachedState> CACHE = new ConcurrentHashMap<>();

    /**
     * Result is cached per world for the current server tick, so repeated calls
     * from multiple players in the same world only compute the average once.
     */
    public static boolean isLevelIdle(ServerLevel level) {
        int currentTick = MinecraftServer.currentTick;
        CachedState cached = CACHE.get(level);
        if (cached != null && cached.tick == currentTick) {
            return cached.idle;
        }

        boolean idle = computeIdle(level);
        CACHE.put(level, new CachedState(currentTick, idle));
        return idle;
    }

    private static boolean computeIdle(ServerLevel level) {
        long[] times = level.tickTimes1m.getTimes();
        long total = 0L;
        int count = 0;

        for (long value : times) {
            if (value > 0L) {
                total += value;
                count++;
            }
        }

        if (count < PrefetchChunks.minSamples) {
            return false;
        }

        double msptMillis = (double) total / (double) count * 1.0E-6D;
        return msptMillis <= PrefetchChunks.msptThreshold;
    }

    private record CachedState(int tick, boolean idle) {
    }
}
