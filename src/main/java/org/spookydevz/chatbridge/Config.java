package org.spookydevz.chatbridge;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> DISCORD_TOKEN =
            B.comment("Discord Bot Token").define("discord.token", "PUT_DISCORD_BOT_TOKEN_HERE");
    public static final ModConfigSpec.LongValue DISCORD_CHANNEL_ID =
            B.comment("Discord Channel ID").defineInRange("discord.channel_id", 0L, 0L, Long.MAX_VALUE);
    public static final ModConfigSpec.ConfigValue<String> DISCORD_INVITE =
            B.comment("Invite link used by /discord").define("discord.invite", "https://discord.gg/spookydevz");

    public static final ModConfigSpec SPEC = B.build();

    public static void load() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
