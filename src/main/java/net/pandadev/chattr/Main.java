package net.pandadev.chattr;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "chattr",
        name = "Chattr",
        version = "1.0",
        description = "A simple chat plugin for Velocity proxies",
        url = "https://pandadev.net",
        authors = {"PandaDEV"})
public class Main {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    private final Metrics.Factory metricsFactory;

    @Inject
    public Main(Metrics.Factory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Chattr plugin is initializing.");
        server.getCommandManager().register("msg", new Commands.MsgCommand(server), "chat");
        server.getCommandManager().register("r", new Commands.ReplyCommand(), "reply");

        int pluginId = 21956;
        metricsFactory.make(this, pluginId);
    }
}