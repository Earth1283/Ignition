package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;

public class PrefetchChunks extends ConfigModule {

    public String basePath() {
        return ConfigCategory.ASYNC.basePath() + ".chunk-prefetch";
    }

    public static boolean enabled = false;
    public static double msptThreshold = 40.0;
    public static int minSamples = 100;
    public static int chunksAhead = 2;
    public static double minSpeedBlocksPerTick = 0.4;
    public static double headingDivergenceCancelDegrees = 45.0;
    public static int maxOutstandingPerPlayer = 6;
    public static int ticketTtlTicks = 100;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(),
            "Speculatively pre-loads/generates chunks ahead of a player's movement direction during idle server time. Experimental.",
            "在服务器空闲时, 根据玩家移动方向预测性地预加载/预生成区块. 实验性功能.");

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled,
            globalConfig.pickStringRegionBased(
                "Master toggle for chunk prefetching.",
                "区块预取功能总开关."));

        globalConfig.addCommentRegionBased(basePath() + ".load-gate",
            "Controls when prefetching is allowed to run based on current server load.",
            "根据当前服务器负载控制是否允许预取运行.");

        msptThreshold = globalConfig.getDouble(basePath() + ".load-gate.mspt-threshold", msptThreshold,
            globalConfig.pickStringRegionBased(
                "Only prefetch when this world's 1-minute average MSPT is at or below this value. Lower = more conservative.",
                "仅当该世界最近1分钟平均MSPT不高于此值时才进行预取. 数值越低越保守."));

        minSamples = globalConfig.getInt(basePath() + ".load-gate.min-samples", minSamples,
            globalConfig.pickStringRegionBased(
                "Minimum number of non-zero MSPT samples required before the average is trusted. Below this (e.g. shortly after startup), prefetching stays off.",
                "在平均值被信任前所需的最小非零MSPT样本数. 低于此值时 (例如服务器刚启动时) 预取功能将保持关闭."));

        globalConfig.addCommentRegionBased(basePath() + ".prediction",
            "Controls how far ahead and under what conditions chunks are predicted and requested.",
            "控制预测和请求区块的提前范围及触发条件.");

        chunksAhead = globalConfig.getInt(basePath() + ".prediction.chunks-ahead", chunksAhead,
            globalConfig.pickStringRegionBased(
                "How many chunks beyond the player's view distance to prefetch, along their predicted heading.",
                "沿玩家预测朝向, 在视距之外预取的区块数量."));

        minSpeedBlocksPerTick = globalConfig.getDouble(basePath() + ".prediction.min-speed-blocks-per-tick", minSpeedBlocksPerTick,
            globalConfig.pickStringRegionBased(
                "Minimum horizontal speed (blocks/tick) before prediction activates. Prevents wasted work for stationary or slow-moving players.",
                "预测功能激活所需的最小水平移动速度 (方块/tick). 用于避免为静止或缓慢移动的玩家浪费计算."));

        headingDivergenceCancelDegrees = globalConfig.getDouble(basePath() + ".prediction.heading-divergence-cancel-degrees", headingDivergenceCancelDegrees,
            globalConfig.pickStringRegionBased(
                "If the player's actual heading diverges from the predicted heading by more than this many degrees, in-flight prefetch requests are cancelled.",
                "若玩家实际朝向与预测朝向的偏差超过此角度 (度), 则取消正在进行的预取请求."));

        globalConfig.addCommentRegionBased(basePath() + ".limits",
            "Backstop limits that apply independently of the load-gate.",
            "独立于负载检测生效的兜底限制.");

        maxOutstandingPerPlayer = globalConfig.getInt(basePath() + ".limits.max-outstanding-per-player", maxOutstandingPerPlayer,
            globalConfig.pickStringRegionBased(
                "Hard cap on outstanding (in-flight) prefetch requests per player.",
                "每位玩家进行中的预取请求数量上限."));

        ticketTtlTicks = globalConfig.getInt(basePath() + ".limits.ticket-ttl-ticks", ticketTtlTicks,
            globalConfig.pickStringRegionBased(
                "Prefetch requests are cancelled if unclaimed after this many ticks.",
                "预取请求在此tick数内未完成则自动取消."));
    }
}
