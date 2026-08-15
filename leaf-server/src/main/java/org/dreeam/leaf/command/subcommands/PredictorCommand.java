package org.dreeam.leaf.command.subcommands;

import net.kyori.adventure.text.Component;
import org.dreeam.leaf.async.chunk.ChunkPrefetchScheduler;
import org.dreeam.leaf.command.LeafCommand;
import org.dreeam.leaf.command.PermissionedLeafSubcommand;
import org.dreeam.leaf.config.modules.async.PrefetchChunks;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class PredictorCommand extends PermissionedLeafSubcommand {

    public static final String LITERAL_ARGUMENT = "predictor";
    public static final String PERM = LeafCommand.BASE_PERM + "." + LITERAL_ARGUMENT;
    private static final DecimalFormat PERCENT = new DecimalFormat("##0.0");

    public PredictorCommand() {
        super(PERM, PermissionDefault.TRUE);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase(Locale.ENGLISH) : "stats";

        switch (mode) {
            case "info" -> sendInfo(sender);
            case "stats" -> sendStats(sender);
            default -> sender.sendMessage(text("Usage: /leaf predictor <info|stats>", RED));
        }

        return true;
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(text("Chunk prefetch predictor", GOLD));
        sender.sendMessage(text("  enabled: ", GRAY).append(text(PrefetchChunks.enabled, PrefetchChunks.enabled ? GREEN : RED)));
        sender.sendMessage(text("  mspt threshold: ", GRAY).append(text(PrefetchChunks.msptThreshold + "ms", AQUA)));
        sender.sendMessage(text("  min samples: ", GRAY).append(text(PrefetchChunks.minSamples, AQUA)));
        sender.sendMessage(text("  chunks ahead: ", GRAY).append(text(PrefetchChunks.chunksAhead, AQUA)));
        sender.sendMessage(text("  min speed: ", GRAY).append(text(PrefetchChunks.minSpeedBlocksPerTick + " blocks/tick", AQUA)));
        sender.sendMessage(text("  divergence cancel angle: ", GRAY).append(text(PrefetchChunks.headingDivergenceCancelDegrees + "°", AQUA)));
        sender.sendMessage(text("  max outstanding/player: ", GRAY).append(text(PrefetchChunks.maxOutstandingPerPlayer, AQUA)));
        sender.sendMessage(text("  ticket ttl: ", GRAY).append(text(PrefetchChunks.ticketTtlTicks + " ticks", AQUA)));
    }

    private void sendStats(CommandSender sender) {
        long issued = ChunkPrefetchScheduler.getIssuedCount();
        long hits = ChunkPrefetchScheduler.getHitCount();
        long misses = ChunkPrefetchScheduler.getMissCount();
        long cancelled = ChunkPrefetchScheduler.getCancelledCount();
        double hitRate = ChunkPrefetchScheduler.getHitRate();

        sender.sendMessage(text("Chunk prefetch predictor stats", GOLD));
        sender.sendMessage(text("  tracked players: ", GRAY).append(text(ChunkPrefetchScheduler.getTrackedPlayerCount(), AQUA)));
        sender.sendMessage(text("  outstanding requests: ", GRAY).append(text(ChunkPrefetchScheduler.getOutstandingCount(), AQUA)));
        sender.sendMessage(text("  issued: ", GRAY).append(text(issued, AQUA)));
        sender.sendMessage(text("  hits: ", GRAY).append(text(hits, GREEN))
            .append(text("  misses: ", GRAY)).append(text(misses, RED))
            .append(text("  cancelled: ", GRAY)).append(text(cancelled, YELLOW)));
        sender.sendMessage(text("  hit rate: ", GRAY).append(getColoredRate(hitRate)));
        sender.sendMessage(text("(hit = predicted chunk was actually reached before its ticket expired; cancelled = dropped from tracking after a heading change, not counted against accuracy - the underlying chunk load is not cancelled and still completes)", DARK_GRAY));
    }

    private static Component getColoredRate(double rate) {
        double percent = rate * 100.0;
        return text(PERCENT.format(percent) + "%",
            percent >= 70 ? GREEN :
                percent >= 40 ? YELLOW :
                    RED);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String subCommand, final String[] args) {
        if (args.length == 1) {
            return List.of("info", "stats");
        }

        return Collections.emptyList();
    }
}
