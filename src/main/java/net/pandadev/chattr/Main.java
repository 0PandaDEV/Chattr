package net.pandadev.chattr;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(
        id = "chattr",
        name = "Chattr",
        version = "1.0",
        description = "A simple chat plugin for Velocity proxies",
        url = "https://pandadev.net",
        authors = {"PandaDEV"}
)
public class Main {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Chattr plugin is initializing.");
        server.getCommandManager().register("msg", new MsgCommand(), "chat");
    }

    public class MsgCommand implements SimpleCommand {
        private final MiniMessage miniMessage = MiniMessage.miniMessage();

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            if (args.length < 2) {
                source.sendMessage(Component.text("Usage: /msg <player> <message>", NamedTextColor.RED));
                return;
            }

            String targetPlayerName = args[0];
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Optional<Player> targetPlayer = server.getPlayer(targetPlayerName);
            if (targetPlayer.isPresent()) {
                String formattedMsgTo = "<#ff6f00>[MSG TO] <dark_gray>»</dark_gray> <white>" + targetPlayer.get().getUsername() + ":</white> <#ff6f00>" + message;
                String formattedMsgFrom = "<#ff6f00>[MSG From] <dark_gray>»</dark_gray> <white>" + (source instanceof Player ? ((Player) source).getUsername() : "<bold>Server</bold>") + ":</white> <#ff6f00>" + message;

                source.sendMessage(miniMessage.deserialize(formattedMsgTo));
                targetPlayer.get().sendMessage(miniMessage.deserialize(formattedMsgFrom));
            } else {
                source.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            }
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();

            if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
                return server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.toList());
            } else if (args.length == 1) {
                return server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }
}

