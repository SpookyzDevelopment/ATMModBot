package org.spookydevz.chatbridge;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordBridge extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordBridge.class);
    private static JDA jda;
    private static long channelId = 0L;
    private static boolean isConnected = false;

    public static synchronized void start(String token, long chanId) {
        stop();
        
        if (token == null || token.isBlank() || token.equals("PUT_YOUR_DISCORD_BOT_TOKEN_HERE") || chanId == 0L) {
            LOGGER.warn("Discord bridge disabled - missing token or channel ID");
            LOGGER.info("Please configure your Discord bot token and channel ID in the config file");
            return;
        }
        
        channelId = chanId;
        
        try {
            LOGGER.info("Starting Discord bridge...");
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .setActivity(Activity.playing("Minecraft"))
                    .addEventListeners(new DiscordBridge())
                    .build();
            
            jda.awaitReady();
            isConnected = true;
            
            // Update activity with player count
            updateActivity();
            
            LOGGER.info("Discord bridge connected successfully!");
            sendToDiscord("🟢 **" + Config.SERVER_NAME.get() + "** is now online!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bridge: {}", e.getMessage());
            isConnected = false;
        }
    }

    public static synchronized void stop() {
        if (jda != null) {
            try {
                if (isConnected) {
                    sendToDiscord("🔴 **" + Config.SERVER_NAME.get() + "** is shutting down...");
                    Thread.sleep(1000); // Give time for message to send
                }
                jda.shutdownNow();
                LOGGER.info("Discord bridge stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping Discord bridge: {}", e.getMessage());
            }
            jda = null;
            isConnected = false;
        }
    }

    public static void sendToDiscord(String content) {
        if (!isConnected || jda == null || channelId == 0L) return;
        
        try {
            MessageChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                // Split long messages to avoid Discord's 2000 character limit
                if (content.length() > 2000) {
                    content = content.substring(0, 1997) + "...";
                }
                
                channel.sendMessage(content).queue(
                    success -> {}, // Success - do nothing
                    error -> LOGGER.warn("Failed to send Discord message: {}", error.getMessage())
                );
            } else {
                LOGGER.warn("Discord channel not found with ID: {}", channelId);
            }
        } catch (Exception e) {
            LOGGER.error("Error sending message to Discord: {}", e.getMessage());
        }
    }
    
    public static void updateActivity() {
        if (!isConnected || jda == null) return;
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                int playerCount = server.getPlayerCount();
                int maxPlayers = server.getMaxPlayers();
                jda.getPresence().setActivity(Activity.playing(playerCount + "/" + maxPlayers + " players"));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to update Discord activity: {}", e.getMessage());
        }
    }
    
    public static boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) return;
        
        // Only listen to the configured channel
        if (event.getChannel().getIdLong() != channelId) return;
        
        String username = event.getAuthor().getName();
        String message = event.getMessage().getContentDisplay();
        
        // Ignore empty messages
        if (message.trim().isEmpty()) return;
        
        // Format message for Minecraft
        String minecraftMessage = "§9[Discord] §f<" + username + "> " + message;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                try {
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal(minecraftMessage), false
                    );
                    LOGGER.info("[Discord -> MC] <{}> {}", username, message);
                } catch (Exception e) {
                    LOGGER.error("Error broadcasting Discord message to Minecraft: {}", e.getMessage());
                }
            });
        }
    }
}