package net.pandadev.chattr;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final Path configPath;
    private ConfigurationNode root;

    private String msgToTemplate;
    private String msgFromTemplate;
    private String replyToTemplate;
    private String replyFromTemplate;

    public ConfigManager(Path dataFolder) {
        this.configPath = dataFolder.resolve("config.yml");
    }

    public void load() throws IOException {
        if (Files.notExists(configPath)) {
            Files.createDirectories(configPath.getParent());
            try (var in = getClass().getResourceAsStream("/config.yml")) {
                Files.copy(in, configPath);
            }
        }

        var loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        root = loader.load();

        boolean changed = false;

        changed |= setDefault("messages", "privateMessageTo",
                "<#ff6f00>[MSG To] <dark_gray>•</dark_gray> <white>{receiver}</white> <#ff6f00>{message}");
        changed |= setDefault("messages", "privateMessageFrom",
                "<#ff6f00>[MSG From] <dark_gray>•</dark_gray> <white>{sender}</white> <#ff6f00>{message}");
        changed |= setDefault("messages", "replyMessageTo",
                "<#ff6f00>[Reply To] <dark_gray>•</dark_gray> <white>{receiver}</white> <#ff6f00>{message}");
        changed |= setDefault("messages", "replyMessageFrom",
                "<#ff6f00>[Reply From] <dark_gray>•</dark_gray> <white>{sender}</white> <#ff6f00>{message}");

        msgToTemplate = root.node("messages", "privateMessageTo").getString();
        msgFromTemplate = root.node("messages", "privateMessageFrom").getString();
        replyToTemplate = root.node("messages", "replyMessageTo").getString();
        replyFromTemplate = root.node("messages", "replyMessageFrom").getString();

        if (changed) {
            loader.save(root);
        }
    }

    private boolean setDefault(String category, String key, String defaultValue) {
        var node = root.node(category, key);
        if (node.virtual() || node.getString() == null) {
            try {
                node.set(defaultValue);
            } catch (SerializationException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public void reload() throws IOException {
        load();
    }

    public String getMsgToTemplate() {return msgToTemplate;}
    public String getMsgFromTemplate() {return msgFromTemplate;}
    public String getReplyToTemplate() {return replyToTemplate;}
    public String getReplyFromTemplate() {return replyFromTemplate;}
}
