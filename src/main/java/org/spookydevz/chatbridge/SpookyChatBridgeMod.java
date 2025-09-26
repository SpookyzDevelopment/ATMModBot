package org.spookydevz.chatbridge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.MinecraftForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SpookyChatBridgeMod.MODID)
public class SpookyChatBridgeMod {
    public static final String MODID = "spooky_chatbridge";
    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyChatBridgeMod.class);

    public SpookyChatBridgeMod(IEventBus modBus) {
        LOGGER.info("Initializing SpookyDevz ChatBridge...");
        
        modBus.addListener(this::setup);
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Setting up SpookyDevz ChatBridge configuration...");
        Config.register();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, initializing Discord bridge...");
        DiscordBridge.start(Config.DISCORD_TOKEN.get(), Config.DISCORD_CHANNEL_ID.getLongValue());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, shutting down Discord bridge...");
        DiscordBridge.stop();
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        try {
            String playerName = event.getUsername();
            String message = event.getMessage().getString();
            
            // Format message for Discord
            String discordMessage = "**" + playerName + "**: " + message;
            DiscordBridge.sendToDiscord(discordMessage);
            
            LOGGER.info("[MC -> Discord] <{}> {}", playerName, message);
        } catch (Exception e) {
            LOGGER.error("Error handling chat event: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.ENABLE_JOIN_LEAVE_MESSAGES.get()) return;
        
        try {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            String message = "➡️ **" + player.getName().getString() + "** joined the server";
            DiscordBridge.sendToDiscord(message);
            
            // Update Discord activity with new player count
            DiscordBridge.updateActivity();
        } catch (Exception e) {
            LOGGER.error("Error handling player join event: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!Config.ENABLE_JOIN_LEAVE_MESSAGES.get()) return;
        
        try {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            String message = "⬅️ **" + player.getName().getString() + "** left the server";
            DiscordBridge.sendToDiscord(message);
            
            // Update Discord activity with new player count
            DiscordBridge.updateActivity();
        } catch (Exception e) {
            LOGGER.error("Error handling player leave event: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!Config.ENABLE_DEATH_MESSAGES.get()) return;
        
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                Component deathMessage = player.getCombatTracker().getDeathMessage();
                String message = "💀 " + deathMessage.getString();
                DiscordBridge.sendToDiscord(message);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling death event: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementProgressEvent event) {
        if (!Config.ENABLE_ADVANCEMENT_MESSAGES.get()) return;
        
        try {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            
            // Only send message when advancement is completed
            if (event.getProgressType() == AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT) {
                String advancementTitle = event.getAdvancement().getDisplay() != null ? 
                    event.getAdvancement().getDisplay().getTitle().getString() : "Unknown Achievement";
                
                String message = "🏆 **" + player.getName().getString() + "** has made the advancement **" + advancementTitle + "**";
                DiscordBridge.sendToDiscord(message);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling advancement event: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands...");
        CommandHandler.register(event.getDispatcher());
    }
}