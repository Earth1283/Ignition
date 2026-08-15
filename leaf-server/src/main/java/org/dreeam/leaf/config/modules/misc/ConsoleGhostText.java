package org.dreeam.leaf.config.modules.misc;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;

public class ConsoleGhostText extends ConfigModule {

    public String basePath() {
        return ConfigCategory.MISC.basePath() + ".console-ghost-text";
    }

    public static boolean enabled = false;
    public static boolean forceEnableOnUnsupportedTerminal = false;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Shows a dimmed "ghost text" suggestion ahead of the cursor as you type in the server console,
                predicted from the closest match in your console command history (fish/zsh-shell style).
                Press the Right arrow or End to accept the suggestion.
                Requires a real interactive terminal (a PTY) that supports ANSI escape codes. If the console is
                a dumb terminal or a plain stdin/stdout pipe (e.g. some process managers, IDE run consoles, or
                piping the console through another program), this will not work and a warning will be logged
                on startup instead of enabling it.""",
            """
                在服务器控制台输入时, 于光标前方以暗淡的"幽灵文本"显示建议内容, 该建议基于控制台命令历史记录中
                最匹配的一条命令预测得出 (类似 fish/zsh shell 的效果). 按右方向键或 End 键可采纳该建议.
                需要真正支持 ANSI 转义序列的交互式终端(PTY). 若控制台为哑终端或纯 stdin/stdout 管道
                (例如部分进程管理器、IDE 运行控制台, 或通过其他程序转发控制台输出), 该功能将无法工作,
                启动时将记录一条警告日志, 且不会启用该功能.""");

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled);

        forceEnableOnUnsupportedTerminal = globalConfig.getBoolean(basePath() + ".force-enable-on-unsupported-terminal", forceEnableOnUnsupportedTerminal,
            globalConfig.pickStringRegionBased("""
                    Force-enables ghost text even when the console terminal is detected as not supporting ANSI
                    escape codes (a dumb terminal or a plain stdin/stdout pipe). Only turn this on if you are
                    certain your terminal actually does support ANSI codes despite being detected otherwise
                    (some wrappers/log pipes misreport the terminal type).
                    WARNING: if your terminal genuinely does not support ANSI codes, forcing this on will make
                    the console output look broken, since the raw ANSI escape sequences used to draw the ghost
                    text will be printed as literal garbage characters instead of being interpreted.
                    Do NOT enable this if you run the server in the plain Windows Command Prompt (cmd.exe) -
                    legacy cmd.exe does not interpret ANSI codes and this WILL corrupt your console output.
                    Windows Terminal or PowerShell 7+ are fine.""",
                """
                    即使检测到控制台终端不支持 ANSI 转义序列(哑终端或纯 stdin/stdout 管道), 也强制启用幽灵文本.
                    仅在你确定终端实际支持 ANSI 代码, 只是被误判为不支持时(部分包装器/日志管道会错误报告终端类型)
                    再启用此项.
                    警告: 如果你的终端确实不支持 ANSI 代码, 强制启用该选项会导致控制台输出显示异常, 因为用于绘制
                    幽灵文本的原始 ANSI 转义序列将被当作字面乱码字符直接打印, 而不会被正确解析.
                    如果你在纯粹的 Windows 命令提示符(cmd.exe)中运行服务器, 请勿启用此选项 - 传统 cmd.exe
                    不会解析 ANSI 代码, 这会导致控制台输出损坏. Windows Terminal 或 PowerShell 7+ 则不受影响."""));
    }
}
