package org.spookydevz.chatbridge;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

public class DiscordBridge extends ListenerAdapter {
    private static net.dv8tion.jda.api.JDA JDA;
    private static long channelId = 0L;

    public static synchronized void start(String token, long chanId) {
        stop();
        if (token == null || token.isBlank() || chanId == 0L) {
            System.out.println("[SpookyChatBridge] Discord disabled (missing token or channel id).");
            return;
        }
        channelId = chanId;
        try {
            JDA = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordBridge())
                    .build();
            System.out.println("[SpookyChatBridge] Discord bridge started.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void stop() {
        if (JDA != null) {
            try { JDA.shutdownNow(); } catch (Exception ignored) {}
            JDA = null;
            System.out.println("[SpookyChatBridge] Discord bridge stopped.");
        }
    }

    public static void sendToDiscord(String content) {
        if (JDA == null || channelId == 0L) return;
        MessageChannel ch = JDA.getTextChannelById(channelId);
        if (ch != null) ch.sendMessage(content).queue();
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
                    net.minecraft.network.chat.Component.literal(msg), false
                )
            );
        }
    }
}
