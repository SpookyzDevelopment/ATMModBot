package org.spookydevz.chatbridge;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

public class DiscordBridge extends ListenerAdapter {
    private static JDA jda;
    private static long channelId = 0L;

    public static synchronized void start(String token, long chanId) {
        stop();
        if (token == null || token.isBlank() || token.equals("PUT_DISCORD_BOT_TOKEN_HERE") || chanId == 0L) {
            System.out.println("[SpookyChatBridge] Discord disabled (missing token or channel id).");
            return;
        }
        channelId = chanId;
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordBridge())
                    .build();
            jda.awaitReady(); // Wait for JDA to be ready
            System.out.println("[SpookyChatBridge] Discord bridge started.");
        } catch (Exception e) {
            System.err.println("[SpookyChatBridge] Failed to start Discord bridge:");
            e.printStackTrace();
        }
    }

    public static synchronized void stop() {
        if (jda != null) {
            try { 
                jda.shutdownNow(); 
            } catch (Exception e) {
                System.err.println("[SpookyChatBridge] Error stopping Discord bridge:");
                e.printStackTrace();
            }
            jda = null;
            System.out.println("[SpookyChatBridge] Discord bridge stopped.");
        }
    }

    public static void sendToDiscord(String content) {
        if (jda == null || channelId == 0L) return;
        try {
            MessageChannel ch = jda.getTextChannelById(channelId);
            if (ch != null) {
                ch.sendMessage(content).queue(
                    success -> {}, // Success callback
                    error -> System.err.println("[SpookyChatBridge] Failed to send message to Discord: " + error.getMessage())
                );
            }
        } catch (Exception e) {
            System.err.println("[SpookyChatBridge] Error sending message to Discord:");
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannel().getIdLong() != channelId) return;
        
        String msg = "[Discord] " + event.getAuthor().getName() + ": " + event.getMessage().getContentDisplay();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() ->
                server.getPlayerList().broadcastSystemMessage(
                    Component.literal(msg), false
                )
            );
        }
    }
}
