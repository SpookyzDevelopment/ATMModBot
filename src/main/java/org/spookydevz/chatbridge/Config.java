package org.spookydevz.chatbridge;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> DISCORD_TOKEN =
            BUILDER.comment("Discord Bot Token - Get this from https://discord.com/developers/applications")
                   .define("discord_token", "PUT_YOUR_DISCORD_BOT_TOKEN_HERE");
    
    public static final ModConfigSpec.LongValue DISCORD_CHANNEL_ID =
            BUILDER.comment("Discord Channel ID - Right click on your channel and copy ID")
                   .defineInRange("discord_channel_id", 0L, 0L, Long.MAX_VALUE);
    
    public static final ModConfigSpec.ConfigValue<String> DISCORD_INVITE =
            BUILDER.comment("Discord invite link shown by /discord command")
                   .define("discord_invite", "https://discord.gg/your-server");
    
    public static final ModConfigSpec.BooleanValue ENABLE_JOIN_LEAVE_MESSAGES =
            BUILDER.comment("Send player join/leave messages to Discord")
                   .define("enable_join_leave_messages", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_DEATH_MESSAGES =
            BUILDER.comment("Send player death messages to Discord")
                   .define("enable_death_messages", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_ADVANCEMENT_MESSAGES =
            BUILDER.comment("Send player advancement messages to Discord")
                   .define("enable_advancement_messages", true);
    
    public static final ModConfigSpec.ConfigValue<String> SERVER_NAME =
            BUILDER.comment("Server name shown in Discord messages")
                   .define("server_name", "Minecraft Server");

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}