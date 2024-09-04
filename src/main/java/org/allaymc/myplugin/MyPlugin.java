package org.allaymc.myplugin;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.plugin.Plugin;

@Slf4j
public class MyPlugin extends Plugin {
    @Override
    public void onLoad() {
        log.info("MyPlugin loaded!");
    }

    @Override
    public void onEnable() {
        log.info("MyPlugin enabled!");
    }

    @Override
    public void onDisable() {
        log.info("MyPlugin disabled!");
    }
}