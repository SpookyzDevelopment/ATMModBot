package org.spookydevz.chatbridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import com.google.gson.JsonObject;

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

        // Pterodactyl commands
        dispatcher.register(Commands.literal("ptero")
            .requires(src -> src.hasPermission(4))
            .then(Commands.literal("status")
                .executes(ctx -> executePteroStatusCommand(ctx))
            )
            .then(Commands.literal("restart")
                .executes(ctx -> executePteroRestartCommand(ctx))
            )
            .then(Commands.literal("stop")
                .executes(ctx -> executePteroStopCommand(ctx))
            )
            .then(Commands.literal("command")
                .then(Commands.argument("cmd", StringArgumentType.greedyString())
                    .executes(ctx -> executePteroCommandCommand(ctx))
                )
            )
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
    
    private static int executePteroStatusCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        if (!Config.PTERODACTYL_ENABLED.get()) {
            source.sendFailure(Component.literal("Pterodactyl integration is disabled!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        PterodactylClient client = SpookyChatBridgeMod.getPterodactylClient();
        if (client == null) {
            source.sendFailure(Component.literal("Pterodactyl client not initialized!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Fetching server status...")
                .withStyle(ChatFormatting.YELLOW), false);
        
        client.getServerStatus().thenAccept(status -> {
            if (status != null && status.has("attributes")) {
                JsonObject attrs = status.getAsJsonObject("attributes");
                String currentState = attrs.has("current_state") ? attrs.get("current_state").getAsString() : "unknown";
                
                Component message = Component.literal("Server Status: ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(currentState)
                                .withStyle(ChatFormatting.AQUA));
                
                if (attrs.has("resources")) {
                    JsonObject resources = attrs.getAsJsonObject("resources");
                    if (resources.has("memory_bytes") && resources.has("cpu_absolute")) {
                        long memoryMB = resources.get("memory_bytes").getAsLong() / 1024 / 1024;
                        double cpu = resources.get("cpu_absolute").getAsDouble();
                        
                        message = message.append(Component.literal("\nMemory: " + memoryMB + "MB, CPU: " + String.format("%.1f", cpu) + "%")
                                .withStyle(ChatFormatting.GRAY));
                    }
                }
                
                source.sendSuccess(() -> message, false);
            } else {
                source.sendFailure(Component.literal("Failed to fetch server status!")
                        .withStyle(ChatFormatting.RED));
            }
        });
        
        return 1;
    }
    
    private static int executePteroRestartCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        if (!Config.PTERODACTYL_ENABLED.get()) {
            source.sendFailure(Component.literal("Pterodactyl integration is disabled!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        PterodactylClient client = SpookyChatBridgeMod.getPterodactylClient();
        if (client == null) {
            source.sendFailure(Component.literal("Pterodactyl client not initialized!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Sending restart command to Pterodactyl...")
                .withStyle(ChatFormatting.YELLOW), true);
        
        client.setPowerState("restart").thenAccept(success -> {
            if (success) {
                source.sendSuccess(() -> Component.literal("Server restart initiated!")
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                source.sendFailure(Component.literal("Failed to restart server!")
                        .withStyle(ChatFormatting.RED));
            }
        });
        
        return 1;
    }
    
    private static int executePteroStopCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        if (!Config.PTERODACTYL_ENABLED.get()) {
            source.sendFailure(Component.literal("Pterodactyl integration is disabled!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        PterodactylClient client = SpookyChatBridgeMod.getPterodactylClient();
        if (client == null) {
            source.sendFailure(Component.literal("Pterodactyl client not initialized!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Sending stop command to Pterodactyl...")
                .withStyle(ChatFormatting.YELLOW), true);
        
        client.setPowerState("stop").thenAccept(success -> {
            if (success) {
                source.sendSuccess(() -> Component.literal("Server stop initiated!")
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                source.sendFailure(Component.literal("Failed to stop server!")
                        .withStyle(ChatFormatting.RED));
            }
        });
        
        return 1;
    }
    
    private static int executePteroCommandCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String command = StringArgumentType.getString(ctx, "cmd");
        
        if (!Config.PTERODACTYL_ENABLED.get()) {
            source.sendFailure(Component.literal("Pterodactyl integration is disabled!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        PterodactylClient client = SpookyChatBridgeMod.getPterodactylClient();
        if (client == null) {
            source.sendFailure(Component.literal("Pterodactyl client not initialized!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Sending command to Pterodactyl: " + command)
                .withStyle(ChatFormatting.YELLOW), true);
        
        client.sendCommand(command).thenAccept(success -> {
            if (success) {
                source.sendSuccess(() -> Component.literal("Command sent successfully!")
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                source.sendFailure(Component.literal("Failed to send command!")
                        .withStyle(ChatFormatting.RED));
            }
        });
        
        return 1;
    }
}
