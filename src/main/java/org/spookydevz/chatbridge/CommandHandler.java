package org.spookydevz.chatbridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class CommandHandler {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Discord invite command
        dispatcher.register(Commands.literal("discord")
            .executes(CommandHandler::executeDiscordCommand)
        );

        // Reload command (OP only)
        dispatcher.register(Commands.literal("chatbridge")
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(4))
                .executes(CommandHandler::executeReloadCommand)
            )
            .then(Commands.literal("status")
                .requires(src -> src.hasPermission(4))
                .executes(CommandHandler::executeStatusCommand)
            )
            .then(Commands.literal("test")
                .requires(src -> src.hasPermission(4))
                .executes(CommandHandler::executeTestCommand)
            )
        );
    }
    
    private static int executeDiscordCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String inviteUrl = Config.DISCORD_INVITE.get();
        
        Component message = Component.literal("🎮 Join our Discord server: ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(inviteUrl)
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, inviteUrl))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                        Component.literal("Click to open Discord invite").withStyle(ChatFormatting.YELLOW)))
                        )
                );
        
        source.sendSuccess(() -> message, false);
        return 1;
    }
    
    private static int executeReloadCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("Reloading Discord bridge...")
                    .withStyle(ChatFormatting.YELLOW), false);
            
            // Stop current bridge
            DiscordBridge.stop();
            
            // Wait a moment
            Thread.sleep(2000);
            
            // Start with new config values
            DiscordBridge.start(Config.DISCORD_TOKEN.get(), Config.DISCORD_CHANNEL_ID.get());
            
            source.sendSuccess(() -> Component.literal("✅ Discord bridge reloaded successfully!")
                    .withStyle(ChatFormatting.GREEN), true);
                    
        } catch (Exception e) {
            source.sendFailure(Component.literal("❌ Failed to reload Discord bridge: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
        
        return 1;
    }
    
    private static int executeStatusCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        boolean connected = DiscordBridge.isConnected();
        String status = connected ? "✅ Connected" : "❌ Disconnected";
        ChatFormatting color = connected ? ChatFormatting.GREEN : ChatFormatting.RED;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        int playerCount = server != null ? server.getPlayerCount() : 0;
        int maxPlayers = server != null ? server.getMaxPlayers() : 0;
        
        Component message = Component.literal("Discord Bridge Status: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(status).withStyle(color))
                .append(Component.literal("\nServer: " + Config.SERVER_NAME.get()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\nPlayers: " + playerCount + "/" + maxPlayers).withStyle(ChatFormatting.GRAY));
        
        source.sendSuccess(() -> message, false);
        return 1;
    }
    
    private static int executeTestCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        if (!DiscordBridge.isConnected()) {
            source.sendFailure(Component.literal("❌ Discord bridge is not connected!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        String testMessage = "🧪 **Test message** from " + Config.SERVER_NAME.get() + " - sent by " + source.getTextName();
        DiscordBridge.sendToDiscord(testMessage);
        
        source.sendSuccess(() -> Component.literal("✅ Test message sent to Discord!")
                .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
}