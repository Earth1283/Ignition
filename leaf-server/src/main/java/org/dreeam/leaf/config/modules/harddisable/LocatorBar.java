package org.dreeam.leaf.config.modules.harddisable;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;

public class LocatorBar extends ConfigModule {

    public String basePath() {
        return ConfigCategory.HARD_DISABLE.basePath() + ".locator-bar";
    }

    public static boolean disable = false;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Hard-disables the locator bar feature entirely, independent of the locatorBar gamerule.
                Unlike Paper's "disable expensive calculations" gamerule optimization, which only skips the
                expensive parts of locator bar tracking, this skips ALL calculations associated with it
                (waypoint/player tracking, connection bookkeeping, etc.) outright. While enabled, players will
                not receive locator bar waypoints and the locatorBar gamerule has no effect.""",
            """
                彻底硬禁用定位栏(locator bar)功能, 不受 locatorBar 游戏规则影响.
                与Paper的"禁用昂贵计算"游戏规则优化不同(那只会跳过定位栏追踪中开销较大的部分计算), 此选项会完全跳过
                与定位栏相关的所有计算(路径点/玩家追踪、连接记录等). 开启后玩家将不会收到任何定位栏路径点数据,
                且 locatorBar 游戏规则将不再生效.""");

        disable = globalConfig.getBoolean(basePath() + ".disable", disable);
    }
}
