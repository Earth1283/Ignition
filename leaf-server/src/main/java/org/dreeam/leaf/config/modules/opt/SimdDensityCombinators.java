package org.dreeam.leaf.config.modules.opt;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;

public class SimdDensityCombinators extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".simd-density-combinators";
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(),
            "Uses the Vector API (AVX/FMA-capable CPUs) to vectorize density-function combinator math during chunk " +
                "generation. Produces bit-identical output to the scalar path.",
            "使用Vector API (适用于支持AVX/FMA的CPU) 对区块生成过程中的密度函数组合运算进行向量化. 输出结果与标量路径完全一致.");

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled,
            globalConfig.pickStringRegionBased(
                "Master toggle for SIMD density combinators.",
                "SIMD密度组合运算功能总开关."));
    }
}
