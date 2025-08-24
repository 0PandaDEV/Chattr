package net.pandadev.chattr;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "chattr",
        name = "Chattr",
        version = "1.0",
        description = "A simple msg plugin for Velocity proxies",
        url = "https://pandadev.net",
        authors = {"PandaDEV"}
)
public class Main {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    private final Metrics.Factory metricsFactory;
    private ConfigManager config;


    @Inject
    public Main(Metrics.Factory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }

    @Inject
    public void initConfig(@com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.config = new ConfigManager(dataDirectory);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Chattr plugin is initializing.");

        try {
            config.load();
        } catch (IOException e) {
            logger.error("Failed to load config.yml", e);
            return;
        }

        server.getCommandManager().register( "msg", new Commands.MsgCommand(server, config),"chat", "tell", "w");
        server.getCommandManager().register("r", new Commands.ReplyCommand(config),"reply");
        server.getCommandManager().register("chattrreload", new Commands.ReloadCommand(this, config));

        int pluginId = 21956;
        metricsFactory.make(this, pluginId);
    }

    public void reloadConfig() throws Exception {
        this.config.reload();
        logger.info("Config reloaded.");
    }
}
