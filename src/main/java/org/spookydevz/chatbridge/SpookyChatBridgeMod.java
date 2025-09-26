package org.spookydevz.chatbridge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.MinecraftForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(SpookyChatBridgeMod.MODID)
public class SpookyChatBridgeMod {
    public static final String MODID = "spooky_chatbridge";

    public SpookyChatBridgeMod(IEventBus modBus) {
        modBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onChat);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void setup(final FMLCommonSetupEvent event) {
        Config.load();
    }

    private void onServerStarted(ServerStartedEvent e) {
        DiscordBridge.start(Config.DISCORD_TOKEN.get(), Config.DISCORD_CHANNEL_ID.getLongValue());
    }

    private void onServerStopping(ServerStoppingEvent e) {
        DiscordBridge.stop();
    }

    private void onChat(ServerChatEvent e) {
        String player = e.getUsername();
        String content = e.getMessage().getString();
        DiscordBridge.sendToDiscord("**" + player + "**: " + content);
    }

    private void onRegisterCommands(RegisterCommandsEvent e) {
        CommandHandler.register(e.getDispatcher());
    }
}
