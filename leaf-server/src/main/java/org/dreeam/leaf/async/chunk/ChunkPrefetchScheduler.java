package org.dreeam.leaf.async.chunk;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.dreeam.leaf.config.modules.async.PrefetchChunks;
import org.dreeam.leaf.world.WorldLoadGate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Speculatively requests chunks ahead of a player's predicted movement, when the
 * world is idle per {@link WorldLoadGate}. Not yet wired to a vanilla chunk-load
 * call site; see {@link #requestPrefetch}.
 */
public final class ChunkPrefetchScheduler {

    private ChunkPrefetchScheduler() {
    }

    private static final Map<ServerPlayer, PlayerState> STATES = new ConcurrentHashMap<>();

    private static final LongAdder issuedCounter = new LongAdder();
    private static final LongAdder hitCounter = new LongAdder();
    private static final LongAdder missCounter = new LongAdder();
    private static final LongAdder cancelledCounter = new LongAdder();

    /**
     * Should be called once per player per tick from the chunk-loader's per-player
     * tick path. No-op unless {@link PrefetchChunks#enabled} and the world is idle.
     */
    public static void maybePrefetch(ServerPlayer player, ServerLevel level) {
        if (!PrefetchChunks.enabled) {
            return;
        }

        PlayerState state = STATES.computeIfAbsent(player, p -> new PlayerState());
        ChunkPos currentChunk = player.chunkPosition();
        int viewDistance = level.getCraftServer().getViewDistance();
        recordArrivals(state, currentChunk, viewDistance);
        expireOutstanding(state);

        if (!WorldLoadGate.isLevelIdle(level)) {
            return;
        }

        Vec3 position = player.position();
        Vec3 delta = position.subtract(state.lastPosition);
        state.lastPosition = position;

        double speed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (speed < PrefetchChunks.minSpeedBlocksPerTick) {
            return;
        }

        double heading = Math.atan2(delta.z, delta.x);
        if (state.hasHeading && headingDiverges(state.heading, heading)) {
            cancelledCounter.add(state.outstanding.size());
            state.outstanding.clear();
        }
        state.heading = heading;
        state.hasHeading = true;

        if (state.outstanding.size() >= PrefetchChunks.maxOutstandingPerPlayer) {
            return;
        }

        int dirX = (int) Math.round(Math.cos(heading));
        int dirZ = (int) Math.round(Math.sin(heading));

        for (int step = 1; step <= PrefetchChunks.chunksAhead && state.outstanding.size() < PrefetchChunks.maxOutstandingPerPlayer; step++) {
            int targetX = currentChunk.x() + dirX * (viewDistance + step);
            int targetZ = currentChunk.z() + dirZ * (viewDistance + step);
            requestPrefetch(level, player, state, targetX, targetZ);
        }
    }

    /** True prediction accuracy: fraction of prefetch requests the player actually reached before expiry. */
    public static double getHitRate() {
        long hits = hitCounter.sum();
        long resolved = hits + missCounter.sum();
        return resolved == 0 ? 0.0 : (double) hits / (double) resolved;
    }

    public static long getIssuedCount() {
        return issuedCounter.sum();
    }

    public static long getHitCount() {
        return hitCounter.sum();
    }

    public static long getMissCount() {
        return missCounter.sum();
    }

    public static long getCancelledCount() {
        return cancelledCounter.sum();
    }

    public static int getOutstandingCount() {
        int total = 0;
        for (PlayerState state : STATES.values()) {
            total += state.outstanding.size();
        }
        return total;
    }

    public static int getTrackedPlayerCount() {
        return STATES.size();
    }

    /** Call on player disconnect to release tracked heading/outstanding-request state. */
    public static void removePlayer(ServerPlayer player) {
        STATES.remove(player);
    }

    private static boolean headingDiverges(double previous, double current) {
        double diff = Math.abs(previous - current);
        if (diff > Math.PI) {
            diff = 2 * Math.PI - diff;
        }
        return Math.toDegrees(diff) > PrefetchChunks.headingDivergenceCancelDegrees;
    }

    private static void recordArrivals(PlayerState state, ChunkPos currentChunk, int viewDistance) {
        if (state.outstanding.isEmpty()) {
            return;
        }
        state.outstanding.long2LongEntrySet().removeIf(entry -> {
            long key = entry.getLongKey();
            int dx = Math.abs(ChunkPos.getX(key) - currentChunk.x());
            int dz = Math.abs(ChunkPos.getZ(key) - currentChunk.z());
            if (Math.max(dx, dz) <= viewDistance) {
                hitCounter.increment();
                return true;
            }
            return false;
        });
    }

    private static void expireOutstanding(PlayerState state) {
        if (state.outstanding.isEmpty()) {
            return;
        }
        int currentTick = MinecraftServer.currentTick;
        state.outstanding.long2LongEntrySet().removeIf(entry -> {
            if (entry.getLongValue() <= currentTick) {
                missCounter.increment();
                return true;
            }
            return false;
        });
    }

    private static void requestPrefetch(ServerLevel level, ServerPlayer player, PlayerState state, int chunkX, int chunkZ) {
        long key = ChunkPos.pack(chunkX, chunkZ);
        if (state.outstanding.containsKey(key)) {
            return;
        }
        state.outstanding.put(key, MinecraftServer.currentTick + PrefetchChunks.ticketTtlTicks);
        issuedCounter.increment();
        // Submission pending: moonrise's ChunkTaskScheduler.scheduleChunkTask exists (see 0192) but its lowest
        // non-blocking Priority constant needs confirming against the applied tree before wiring this call in.
    }

    private static final class PlayerState {
        Vec3 lastPosition = Vec3.ZERO;
        double heading;
        boolean hasHeading;
        final Long2LongMap outstanding = new Long2LongOpenHashMap();
    }
}
