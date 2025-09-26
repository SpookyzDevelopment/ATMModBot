package org.spookydevz.chatbridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

public class CommandHandler {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discord")
            .executes(ctx -> {
                return executeDiscordCommand(ctx);
            })
        );

        dispatcher.register(Commands.literal("sdreload")
            .requires(src -> src.hasPermission(4))
            .executes(ctx -> {
                return executeReloadCommand(ctx);
            })
        );
    }
    
    private static int executeDiscordCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String inviteUrl = Config.DISCORD_INVITE.get();
        
        Component message = Component.literal("Join our Discord: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(inviteUrl)
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, inviteUrl))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                        Component.literal("Click to open Discord").withStyle(ChatFormatting.YELLOW)))
                        )
                );
        
        source.sendSuccess(() -> message, false);
        return 1;
    }
    
    private static int executeReloadCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            DiscordBridge.stop();
            // Note: Config reloading in NeoForge happens automatically when the config file changes
            // We'll restart with current config values
            DiscordBridge.start(Config.DISCORD_TOKEN.get(), Config.DISCORD_CHANNEL_ID.getLongValue());
            source.sendSuccess(() -> Component.literal("SpookyDevz ChatBridge reloaded successfully!")
                    .withStyle(ChatFormatting.GREEN), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reload ChatBridge: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            e.printStackTrace();
        }
        
        return 1;
    }
}
