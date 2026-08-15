package org.dreeam.leaf.config.modules.harddisable;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;
import org.dreeam.leaf.util.RecipeBookUnlockMode;

public class RecipeBookUnlocking extends ConfigModule {

    public String basePath() {
        return ConfigCategory.HARD_DISABLE.basePath() + ".recipe-book-unlocking";
    }

    public static RecipeBookUnlockMode mode = RecipeBookUnlockMode.DEFAULT;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Hard-disables recipe book unlocking bookkeeping (per-craft/smelt/knowledge-book discovery
                checks, the PlayerRecipeDiscoverEvent call and the resulting recipe display/packet work),
                independent of any plugin or datapack recipe setup.""",
            """
                彻底硬禁用配方书解锁的相关记录 (每次合成/烧炼/使用知识之书时的解锁检查、PlayerRecipeDiscoverEvent
                事件调用以及随之而来的配方显示/数据包处理), 不受任何插件或数据包配方配置影响.""");

        String modeName = globalConfig.getString(basePath() + ".mode", mode.name(),
            globalConfig.pickStringRegionBased(
                "Available modes: DEFAULT (vanilla behavior), NO_OP (never unlock new recipes), UNLOCK_ALL (every recipe is always shown as unlocked)",
                "可用模式: DEFAULT (原版行为), NO_OP (永不解锁新配方), UNLOCK_ALL (所有配方始终显示为已解锁)"));

        RecipeBookUnlockMode parsed = RecipeBookUnlockMode.fromString(modeName);
        if (parsed == null) {
            LOGGER.error("Unknown recipe book unlock mode {}! Falling back to DEFAULT.", modeName);
            parsed = RecipeBookUnlockMode.DEFAULT;
        }
        mode = parsed;
    }
}
