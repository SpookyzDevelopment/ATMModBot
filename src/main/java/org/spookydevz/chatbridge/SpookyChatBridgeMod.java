package org.spookydevz.chatbridge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.MinecraftForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SpookyChatBridgeMod.MODID)
public class SpookyChatBridgeMod {
    public static final String MODID = "spooky_chatbridge";
    private static final Logger LOGGER = LoggerFactory.getLogger(SpookyChatBridgeMod.class);
    private static PterodactylClient pterodactylClient;

    public SpookyChatBridgeMod(IEventBus modBus) {
        LOGGER.info("Initializing SpookyDevz ChatBridge...");
        
        modBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onChat);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Setting up SpookyDevz ChatBridge configuration...");
        Config.load();
    }

    @SubscribeEvent
    private void onServerStarted(ServerStartedEvent e) {
        LOGGER.info("Server started, initializing Discord bridge...");
        DiscordBridge.start(Config.DISCORD_TOKEN.get(), Config.DISCORD_CHANNEL_ID.getLongValue());
        
        // Initialize Pterodactyl client if enabled
        if (Config.PTERODACTYL_ENABLED.get()) {
            String apiUrl = Config.PTERODACTYL_API_URL.get();
            String apiKey = Config.PTERODACTYL_API_KEY.get();
            String serverId = Config.PTERODACTYL_SERVER_ID.get();
            
            if (!apiUrl.isEmpty() && !apiKey.equals("PUT_PTERODACTYL_API_KEY_HERE") && !serverId.isEmpty()) {
                pterodactylClient = new PterodactylClient(apiUrl, apiKey, serverId);
                LOGGER.info("Pterodactyl client initialized for server: {}", serverId);
            } else {
                LOGGER.warn("Pterodactyl integration enabled but missing configuration!");
            }
        }
    }

    @SubscribeEvent
    private void onServerStopping(ServerStoppingEvent e) {
        LOGGER.info("Server stopping, shutting down Discord bridge...");
        DiscordBridge.stop();
        
        if (pterodactylClient != null) {
            LOGGER.info("Shutting down Pterodactyl client...");
            pterodactylClient.shutdown();
            pterodactylClient = null;
        }
    }

    @SubscribeEvent
    private void onChat(ServerChatEvent e) {
        try {
            String player = e.getUsername();
            String content = e.getMessage().getString();
            String discordMessage = "**" + player + "**: " + content;
            DiscordBridge.sendToDiscord(discordMessage);
        } catch (Exception ex) {
            LOGGER.error("Error handling chat event", ex);
        }
    }

    @SubscribeEvent
    private void onRegisterCommands(RegisterCommandsEvent e) {
        LOGGER.info("Registering commands...");
        CommandHandler.register(e.getDispatcher());
    }
    
    public static PterodactylClient getPterodactylClient() {
        return pterodactylClient;
    }
}
