package net.pandadev.chattr;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
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
        private final ConfigManager config;

        public MsgCommand(ProxyServer server, ConfigManager config) {
            this.server = server;
            this.config = config;
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

                String formattedMsgTo = config.getMsgToTemplate()
                        .replace("{receiver}", target.getUsername())
                        .replace("{message}", message);

                String formattedMsgFrom = config.getMsgFromTemplate()
                        .replace("{sender}", source instanceof Player
                                ? ((Player) source).getUsername()
                                : "<bold>Server</bold>")
                        .replace("{message}", message);

                source.sendMessage(miniMessage.deserialize(formattedMsgTo));
                target.sendMessage(miniMessage.deserialize(formattedMsgFrom));
            }
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();
            List<String> suggestions = new ArrayList<>();

            if (args.length == 0 || args.length == 1) {
                CommandSource finalSource = invocation.source();

                if (finalSource.hasPermission("chattr.msgselector")) {
                    suggestions.add("@a");
                    suggestions.add("@r");
                    suggestions.add("@s");
                }

                suggestions.addAll(server.getAllPlayers().stream()
                        .map(Player::getUsername)
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
            if (selector.startsWith("@") && !source.hasPermission("chattr.msgselector")) {
                return List.of();
            }

            return switch (selector) {
                case "@a" -> new ArrayList<>(server.getAllPlayers());
                case "@r" -> {
                    List<Player> allPlayers = new ArrayList<>(server.getAllPlayers());
                    yield List.of(allPlayers.get(random.nextInt(allPlayers.size())));
                }
                case "@s" -> source instanceof Player player ? List.of(player) : List.of();
                default -> server.getAllPlayers().stream()
                        .filter(player -> player.getUsername().equalsIgnoreCase(selector))
                        .toList();
            };
        }
    }

    public static class ReplyCommand implements SimpleCommand {
        private final ConfigManager config;

        public ReplyCommand(ConfigManager config) {
            this.config = config;
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

            String formattedMsgTo = config.getReplyToTemplate()
                    .replace("{receiver}", target.getUsername())
                    .replace("{message}", message);

            String formattedMsgFrom = config.getReplyFromTemplate()
                    .replace("{sender}", playerSource.getUsername())
                    .replace("{message}", message);

            playerSource.sendMessage(miniMessage.deserialize(formattedMsgTo));
            target.sendMessage(miniMessage.deserialize(formattedMsgFrom));

            lastMessaged.put(target, playerSource);
            lastMessaged.put(playerSource, target);
        }
    }

    public static class ReloadCommand implements SimpleCommand {
        private final ConfigManager config;
        private final Main plugin;

        public ReloadCommand(Main plugin, ConfigManager config) {
            this.plugin = plugin;
            this.config = config;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();

            if (!source.hasPermission("chattr.reload")) {
                source.sendMessage(Component.text("You don't have permission to reload the config.", NamedTextColor.RED));
                return;
            }

            try {
                plugin.reloadConfig();
                source.sendMessage(Component.text("Config reloaded successfully!", NamedTextColor.GREEN));
            } catch (Exception e) {
                source.sendMessage(Component.text("Failed to reload config: " + e.getMessage(), NamedTextColor.RED));
                e.printStackTrace();
            }
        }
    }
}
