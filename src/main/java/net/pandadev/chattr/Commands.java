package net.pandadev.chattr;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.stream.Collectors;

public class Commands {

    private static final Map<Player, Player> lastMessaged = new HashMap<>();
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Random random = new Random();

    public static class MsgCommand implements SimpleCommand {
        private final ProxyServer server;

        public MsgCommand(ProxyServer server) {
            this.server = server;
        }

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

                String formattedMsgTo = "<#ff6f00>[MSG To] <dark_gray>•</dark_gray> <white>" + target.getUsername()
                        + "</white> <#ff6f00>" + message;
                String formattedMsgFrom = "<#ff6f00>[MSG From] <dark_gray>•</dark_gray> <white>"
                        + (source instanceof Player ? ((Player) source).getUsername() : "<bold>Server</bold>")
                        + "</white> <#ff6f00>" + message;

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
                    yield List.of(allPlayers.get(random.nextInt(allPlayers.size())));
                }
                case "@s", "@p" -> source instanceof Player player ? List.of(player) : List.of();
                default -> server.getAllPlayers().stream()
                        .filter(player -> player.getUsername().equalsIgnoreCase(selector))
                        .toList();
            };
        }
    }

    public static class ReplyCommand implements SimpleCommand {
        public ReplyCommand() {

        }

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

            Player target = lastMessaged.get(playerSource);

            if (target == null) {
                playerSource.sendMessage(Component.text("No one to reply to.", NamedTextColor.RED));
                return;
            }

            String message = String.join(" ", args);
            String formattedMsgTo = "<#ff6f00>[Reply To] <dark_gray>•</dark_gray> <white>" + target.getUsername()
                    + "</white> <#ff6f00>" + message;
            String formattedMsgFrom = "<#ff6f00>[Reply From] <dark_gray>•</dark_gray> <white>"
                    + playerSource.getUsername() + "</white> <#ff6f00>" + message;

            playerSource.sendMessage(miniMessage.deserialize(formattedMsgTo));
            target.sendMessage(miniMessage.deserialize(formattedMsgFrom));
            lastMessaged.put(target, playerSource);
            lastMessaged.put(playerSource, target);
        }
    }
}
