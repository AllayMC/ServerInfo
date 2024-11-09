package org.allaymc.serverinfo;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.bossbar.BossBar;
import org.allaymc.api.bossbar.BossBarColor;
import org.allaymc.api.entity.component.attribute.AttributeType;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.entity.EntityTeleportEvent;
import org.allaymc.api.eventbus.event.player.PlayerJoinEvent;
import org.allaymc.api.eventbus.event.player.PlayerQuitEvent;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.scoreboard.Scoreboard;
import org.allaymc.api.scoreboard.data.DisplaySlot;
import org.allaymc.api.server.Server;
import org.allaymc.api.math.MathUtils;
import org.allaymc.api.world.World;
import org.joml.Vector3f;

import java.util.*;

@Slf4j
@Getter
public final class ServerInfo extends Plugin {

    public static ServerInfo INSTANCE;

    private final Map<World, BossBar> BOSS_BARS = new HashMap<>();
    private final Set<EntityPlayer> SCOREBOARD_DISABLED = new HashSet<>();
    private Settings SETTINGS;

    @Override
    public void onLoad() {
        INSTANCE = this;
        log.info("ServerInfo loaded!");
        SETTINGS = ConfigManager.create(Settings.class, config -> {
            config.withConfigurer(new YamlSnakeYamlConfigurer());
            config.withBindFile(pluginContainer.dataFolder().resolve("config.yml"));
            config.withRemoveOrphans(true);
            config.saveDefaults();
            config.load(true);
        });
    }

    @Override
    public void onEnable() {
        log.info("ServerInfo enabled!");
        Server.getInstance().getEventBus().registerListener(this);
        Registries.COMMANDS.register(new ServerInfoCommand());
        if (SETTINGS.showMSPTBar()) {
            Server.getInstance().getScheduler().scheduleRepeating(this, () -> {
                updateMSPTBars();
                return true;
            }, 20);
        }
    }

    @Override
    public void onDisable() {
        log.info("ServerInfo disabled!");
        Server.getInstance().getEventBus().unregisterListener(this);
    }

    public BossBar getOrCreateWorldMSPTBar(World world) {
        return BOSS_BARS.computeIfAbsent(world, name -> BossBar.create());
    }

    public boolean isScoreboardDisabled(EntityPlayer player) {
        return SCOREBOARD_DISABLED.contains(player);
    }

    public void setScoreboardDisabled(EntityPlayer player, boolean disabled) {
        if (disabled) {
            SCOREBOARD_DISABLED.add(player);
        } else {
            SCOREBOARD_DISABLED.remove(player);
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        if (SETTINGS.showMSPTBar()) {
            getOrCreateWorldMSPTBar(player.getWorld()).addViewer(player);
        }

        var scoreboard = new Scoreboard("Dashboard");
        scoreboard.addViewer(player, DisplaySlot.SIDEBAR);

        Server.getInstance().getScheduler().scheduleRepeating(this, () -> {
            if (player.isDisconnected()) {
                return false;
            }
            if (!isScoreboardDisabled(player)) {
                scoreboard.addViewer(player, DisplaySlot.SIDEBAR);
                updateScoreboard(player, scoreboard);
            } else {
                scoreboard.removeViewer(player, DisplaySlot.SIDEBAR);
            }
            return true;
        }, 20);
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (SETTINGS.showMSPTBar()) {
            getOrCreateWorldMSPTBar(player.getWorld()).removeViewer(player);
        }
        SCOREBOARD_DISABLED.remove(player);
    }

    @EventHandler
    private void onPlayerTeleport(EntityTeleportEvent event) {
        if (!event.isTeleportBetweenWorlds()) {
            return;
        }

        if (!(event.getEntity() instanceof EntityPlayer player)) {
            return;
        }

        getOrCreateWorldMSPTBar(event.getFrom().dimension().getWorld()).removeViewer(player);
        getOrCreateWorldMSPTBar(event.getTo().dimension().getWorld()).addViewer(player);
    }

    private void updateMSPTBars() {
        BOSS_BARS.forEach((world, bossbar) -> {
            bossbar.setTitle("TPS: §a" + world.getTPS());
            var mspt = world.getMSPT();
            // Progress should between 0 and 1
            bossbar.setProgress(Math.min(1.0f, mspt / 50.0f));
            if (mspt > 50) {
                bossbar.setColor(BossBarColor.RED);
            } else if (mspt > 40) {
                bossbar.setColor(BossBarColor.PINK);
            } else if (mspt > 25) {
                bossbar.setColor(BossBarColor.YELLOW);
            } else {
                bossbar.setColor(BossBarColor.GREEN);
            }
        });
    }

    private void updateScoreboard(EntityPlayer player, Scoreboard scoreboard) {
        if (!player.isInWorld()) return;

        var lines = new ArrayList<String>();

        if (SETTINGS.showWorldInfo()) {
            // World info
            var worldInfo = "World: §a" + player.getWorld().getWorldData().getName() + "\n§f" +
                            "Time: §a" + player.getWorld().getWorldData().getTime() + "\n§f" +
                            "TPS: §a" + MathUtils.round(player.getWorld().getTPS(), 2) + "\n§f" +
                            "MSPT: §a" + MathUtils.round(player.getWorld().getMSPT(), 2);
            lines.add(worldInfo);
        }

        if (SETTINGS.showMiscInfo()) {
            var itemInHand = player.getItemInHand();

            lines.add(
                    "ItemInHand:\n§a" + itemInHand.getItemType().getIdentifier().path() + (itemInHand.getMeta() != 0 ? ":" + itemInHand.getMeta() : "") + "\n§f" +
                    "StandingOn:\n§a" + player.getBlockStateStandingOn().getBlockType().getIdentifier().path()
            );
        }

        if (SETTINGS.showChunkInfo()) {
            var chunk = player.getCurrentChunk();
            var chunkInfo =
                    "Chunk: §a" + chunk.getX() + "," + chunk.getZ() + "\n§f" +
                    "Loaded: §a" + player.getDimension().getChunkService().getLoadedChunks().size() + "\n§f" +
                    "Loading: §a" + player.getDimension().getChunkService().getLoadingChunks().size() + "\n§f";
            try {
                var floorLoc = player.getLocation().floor(new Vector3f());
                chunkInfo += "Biome:\n§a" + player.getCurrentChunk().getBiome((int) floorLoc.x() & 15, (int) floorLoc.y(), (int) floorLoc.z() & 15).toString().toLowerCase();
            } catch (IllegalArgumentException e) {
                // y coordinate is out of range
                chunkInfo += "Biome: §aN/A";
            }
            lines.add(chunkInfo);
        }

        if (SETTINGS.showPlayerInfo()) {
            var playerInfo = "Ping: §a" + player.getPing() + "\n§f" +
                             "Food: §a" + player.getFoodLevel() + "/" + (int) AttributeType.PLAYER_HUNGER.getMaxValue() + "\n§f" +
                             "Exhaustion: §a" + MathUtils.round(player.getFoodExhaustionLevel(), 2) + "/" + (int) AttributeType.PLAYER_EXHAUSTION.getMaxValue() + "\n§f" +
                             "Saturation: §a" + MathUtils.round(player.getFoodSaturationLevel(), 2) + "/" + (int) AttributeType.PLAYER_SATURATION.getMaxValue() + "\n§f" +
                             "Exp: §a" + player.getExperienceInCurrentLevel() + "/" + player.getRequireExperienceForCurrentLevel();
            lines.add(playerInfo);
        }

        if (SETTINGS.showLightInfo()) {
            var floorLoc = player.getLocation().floor(new Vector3f());
            int x = (int) floorLoc.x;
            int y = (int) floorLoc.y;
            int z = (int) floorLoc.z;
            var lightService = player.getDimension().getLightService();
            var lightInfo = "Itl: §a" + lightService.getInternalLight(x, y, z) + "\n§f" +
                            "Block: §a" + lightService.getBlockLight(x, y, z) + "\n§f" +
                            "Sky: §a" + lightService.getSkyLight(x, y, z) + "\n§f" +
                            "ItlSky: §a" + lightService.getInternalSkyLight(x, y, z) + "\n§f" +
                            "Queue: §a" + lightService.getQueuedUpdateCount();
            lines.add(lightInfo);
        }

        scoreboard.setLines(lines);
    }
}