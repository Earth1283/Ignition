# Predictive Chunk Prefetch

Status: partially implemented. Leaf-side scheduling and gating logic exists; the vanilla
chunk-load call site is not wired in yet (see [Remaining work](#remaining-work)).

## What it does

Speculatively requests chunks ahead of a player's predicted movement direction, using
otherwise-idle server time, so chunks are already loading by the time the player reaches
their view-distance edge. This is different from every other caching mechanism in Leaf
(`ChunkCache`, `NodeEvaluatorCache`, `IterateOutwardsCache`, etc.), which all memoize a
value once it has already been computed. This is the first thing in the codebase that
starts work *before* it's needed.

## Design

### Load gate

`org.dreeam.leaf.world.WorldLoadGate` decides whether a world has spare capacity to
spend on speculative work. It reads `ServerLevel.tickTimes1m` (added by
`0193-SparklyPaper-Track-each-world-MSPT.patch`, a 1200-sample/1-minute ring buffer of
tick durations) and compares the average against a configurable threshold.

Two safety properties that matter here:

- **Fails closed on cold start.** A world with no samples yet (just started) reads as
  `0.0` average MSPT if you don't guard for it, which would look "idle" during the worst
  possible window. The gate requires a minimum sample count before trusting the average.
- **Cached per tick.** Averaging a 1200-element array per player per tick isn't free.
  The result is cached per world, keyed by `MinecraftServer.currentTick`, so every
  player sharing a world only triggers one recomputation per tick.

MSPT alone doesn't prove there's spare *chunk generation* capacity, since chunk workers
run on separate threads from the tick loop. The scheduler backstops this with a hard cap
on outstanding requests per player, independent of the load gate.

### Scheduler

`org.dreeam.leaf.async.chunk.ChunkPrefetchScheduler` tracks each player's position
between calls to derive a heading and speed. If the player is moving fast enough
(configurable minimum, default tuned near sprint speed), it predicts chunk coordinates
along that heading, beyond the player's current view distance, and issues prefetch
requests for them.

Each outstanding request carries a TTL and gets cancelled if the player's heading
diverges past a configurable angle before it resolves — otherwise a player who turns
around would leave orphaned generation work in flight.

### Vanilla integration

The intended call site is `RegionizedPlayerChunkLoader`'s per-player tick path — the
same class `0190-async-chunk-sender.patch` already extends. A patch would add one call,
`ChunkPrefetchScheduler.maybePrefetch(player, level)`, mirroring that precedent.

## Command

`/leaf predictor info` prints the active config values. `/leaf predictor stats` prints
live accuracy: issued/hit/miss/cancelled counts and a hit rate.

A "hit" is counted when a player's actual position later falls within their view distance
of a chunk that was prefetched for them, before its TTL expired — this measures prediction
quality (is the heading/speed extrapolation any good) independently of whether the actual
low-priority chunk-load submission is wired in yet (see below), since a "hit" only requires
that the player really did travel to where the predictor guessed. Cancelled requests (heading
changed) are tracked separately and excluded from the hit rate, since they were abandoned,
not wrong.

## Remaining work

The actual "submit a low-priority chunk load" call inside
`ChunkPrefetchScheduler.requestPrefetch` is a stub. Moonrise exposes
`ChunkTaskScheduler.scheduleChunkTask(x, z, task, priority)` (used with
`Priority.BLOCKING` in `0192-SparklyPaper-Parallel-world-ticking.patch`), but the
lowest non-blocking priority constant needs confirming against the applied source tree
before this can be wired up for real. Once that's known:

1. Submit the prefetch as a chunk-load/generate task at that low priority.
2. Add the `maybePrefetch` call into `RegionizedPlayerChunkLoader` as a new patch
   stacked after `0190` (not a modification of it).
3. Wire `ChunkPrefetchScheduler.removePlayer` into player-quit handling.

## Configuration

`async.chunk-prefetch` in `leaf.yml` (`org.dreeam.leaf.config.modules.async.PrefetchChunks`):

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `false` | Master toggle. Off by default — experimental. |
| `load-gate.mspt-threshold` | `40.0` | Only prefetch when the world's 1-minute average MSPT is at or below this. |
| `load-gate.min-samples` | `100` | Minimum MSPT samples before the average is trusted. |
| `prediction.chunks-ahead` | `2` | Chunks beyond view distance to prefetch along the predicted heading. |
| `prediction.min-speed-blocks-per-tick` | `0.4` | Minimum speed before prediction activates. |
| `prediction.heading-divergence-cancel-degrees` | `45` | Heading change that cancels in-flight requests. |
| `limits.max-outstanding-per-player` | `6` | Hard cap on in-flight requests per player. |
| `limits.ticket-ttl-ticks` | `100` | Ticks before an unclaimed request expires. |

## Possible follow-up: pathfinding continuation

Same idea applied to mob pathfinding: when `PathNavigation.shouldRecomputePath`
(`0318-optimize-PathNavigation-shouldRecomputePath.patch`) keeps returning `false`,
speculatively evaluate the path segment beyond the current tail, pooling evaluators via
the existing `org.dreeam.leaf.async.path.NodeEvaluatorCache`. Not started — sequenced
after chunk prefetch ships and is measured, and should reuse
`AsyncPathfinding`'s thread pool rather than adding a second one.

## Verification (not yet run)

- Compare per-world MSPT (`/leaf mspt`) and spark-profiled chunk-worker time with the
  feature off vs. on.
- Elytra flight at sustained speed across ungenerated terrain: compare chunk pop-in
  timing and worker queue latency.
- Synthetic load spike (mass mob spawn / explosions): confirm prefetch requests drop to
  ~0 once 1-minute MSPT crosses the configured threshold, and resume after recovery.
