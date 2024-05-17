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

import java.util.*;
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

    private final Map<Player, Player> lastMessaged = new HashMap<>();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Chattr plugin is initializing.");
        server.getCommandManager().register("msg", new MsgCommand(), "chat");
        server.getCommandManager().register("r", new ReplyCommand(), "reply");
    }

    public class MsgCommand implements SimpleCommand {
        private final MiniMessage miniMessage = MiniMessage.miniMessage();
        private final Random random = new Random();

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            if (args.length < 2) {
                source.sendMessage(Component.text("Usage: /msg <selector> <message>", NamedTextColor.RED));
                return;
            }

            String selector = args[0];
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            List<Player> targets = resolveSelector(selector, source);

            if (targets.isEmpty()) {
                source.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            for (Player target : targets) {
                if (source instanceof Player playerSource) {
                    lastMessaged.put(target, playerSource);
                    lastMessaged.put(playerSource, target);
                }

                String formattedMsgTo = "<#ff6f00>[MSG To] <dark_gray>•</dark_gray> <white>" + target.getUsername() + "</white> <#ff6f00>" + message;
                String formattedMsgFrom = "<#ff6f00>[MSG From] <dark_gray>•</dark_gray> <white>" + (source instanceof Player ? ((Player) source).getUsername() : "<bold>Server</bold>") + "</white> <#ff6f00>" + message;

                source.sendMessage(miniMessage.deserialize(formattedMsgTo));
                target.sendMessage(miniMessage.deserialize(formattedMsgFrom));
            }
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();
            List<String> suggestions = new ArrayList<>();

            if (args.length == 0 || args.length == 1) {
                suggestions.add("@a");
                suggestions.add("@r");
                suggestions.add("@s");
                suggestions.add("@p");

                CommandSource finalSource = invocation.source();
                suggestions.addAll(server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .filter(username -> !(finalSource instanceof Player && username.equals(((Player) finalSource).getUsername())))
                        .toList());
            }
            if (args.length == 1 && !args[0].isEmpty()) {
                String currentInput = args[0].toLowerCase();
                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(currentInput))
                        .collect(Collectors.toList());
            }
            return suggestions;
        }

        private List<Player> resolveSelector(String selector, CommandSource source) {
            return switch (selector) {
                case "@a" -> new ArrayList<>(server.getAllPlayers());
                case "@r" -> {
                    List<Player> allPlayers = new ArrayList<>(server.getAllPlayers());
                    yield allPlayers.isEmpty() ? Collections.emptyList() : List.of(allPlayers.get(random.nextInt(allPlayers.size())));
                }
                case "@s" -> source instanceof Player ? List.of((Player) source) : Collections.emptyList();
                case "@p" -> {
                    if (source instanceof Player playerSource) {
                        yield server.getAllPlayers().stream()
                                .filter(p -> !p.equals(playerSource))
                                .min(Comparator.comparingDouble(Player::getPing))
                                .map(Collections::singletonList)
                                .orElse(Collections.emptyList());
                    }
                    yield Collections.emptyList();
                }
                default -> server.getPlayer(selector).map(Collections::singletonList).orElse(Collections.emptyList());
            };
        }
    }

    public class ReplyCommand implements SimpleCommand {
        private final MiniMessage miniMessage = MiniMessage.miniMessage();

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            if (!(source instanceof Player playerSource)) {
                source.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                return;
            }

            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /r <message>", NamedTextColor.RED));
                return;
            }

            if (!lastMessaged.containsKey(playerSource)) {
                source.sendMessage(Component.text("No one to reply to.", NamedTextColor.RED));
                return;
            }

            Player target = lastMessaged.get(playerSource);
            String message = String.join(" ", args);
            String formattedMsgTo = "<#ff6f00>[MSG To] <dark_gray>•</dark_gray> <white>" + target.getUsername() + "</white> <#ff6f00>" + message;
            String formattedMsgFrom = "<#ff6f00>[MSG From] <dark_gray>•</dark_gray> <white>" + playerSource.getUsername() + "</white> <#ff6f00>" + message;

            target.sendMessage(miniMessage.deserialize(formattedMsgFrom));
            playerSource.sendMessage(miniMessage.deserialize(formattedMsgTo));
        }
    }
}

