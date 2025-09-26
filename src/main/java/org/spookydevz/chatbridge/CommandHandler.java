package org.spookydevz.chatbridge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public class CommandHandler {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("discord")
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                Component clickable = Component.literal("Join our Discord: " + Config.DISCORD_INVITE.get())
                        .withStyle(s -> s.withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Config.DISCORD_INVITE.get()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open Discord")))
                        );
                src.sendSuccess(() -> clickable, false);
                if (src.getEntity() instanceof ServerPlayer p) p.sendSystemMessage(clickable);
                return 1;
            })
        );

        d.register(Commands.literal("sdreload")
            .requires(src -> src.hasPermission(4))
            .executes(ctx -> {
                DiscordBridge.stop();
                Config.load();
                DiscordBridge.start(Config.DISCORD_TOKEN.get(), Config.DISCORD_CHANNEL_ID.getLongValue());
                ctx.getSource().sendSuccess(() -> Component.literal("SpookyDevz ChatBridge reloaded."), true);
                return 1;
            })
        );
    }
}
