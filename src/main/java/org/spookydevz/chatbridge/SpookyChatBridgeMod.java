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
    }

    @SubscribeEvent
    private void onServerStopping(ServerStoppingEvent e) {
        LOGGER.info("Server stopping, shutting down Discord bridge...");
        DiscordBridge.stop();
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
}
