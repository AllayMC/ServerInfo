package org.allaymc.serverinfo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.entity.component.attribute.AttributeType;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.eventbus.EventHandler;
import org.allaymc.api.eventbus.event.player.PlayerJoinEvent;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.scoreboard.Scoreboard;
import org.allaymc.api.scoreboard.data.DisplaySlot;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.MathUtils;
import org.joml.Vector3f;

import java.util.ArrayList;

@Slf4j
@Getter
public final class ServerInfo extends Plugin {
    public static ServerInfo INSTANCE;

    @Override
    public void onLoad() {
        INSTANCE = this;
        log.info("ServerInfo loaded!");
    }

    @Override
    public void onEnable() {
        log.info("ServerInfo enabled!");
        Server.getInstance().getEventBus().registerListener(this);
    }

    @Override
    public void onDisable() {
        log.info("ServerInfo disabled!");
        Server.getInstance().getEventBus().unregisterListener(this);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var scoreboard = new Scoreboard("Dashboard");
        scoreboard.addViewer(player, DisplaySlot.SIDEBAR);
        Server.getInstance().getScheduler().scheduleRepeating(this, () -> {
            if (player.isDisconnected()) return false;
            updateScoreboard(player, scoreboard);
            return true;
        }, 20);
    }

    private void updateScoreboard(EntityPlayer player, Scoreboard scoreboard) {
        if (!player.isInWorld()) return;

        var lines = new ArrayList<String>();

        // World info
        var worldInfo = "World: §a" + player.getWorld().getWorldData().getName() + "\n§f" +
                        "Time: §a" + player.getWorld().getWorldData().getTime() + "\n§f" +
                        "TPS: §a" + MathUtils.round(player.getWorld().getTps(), 2) + "\n§f" +
                        "MSPT: §a" + MathUtils.round(player.getWorld().getMSPT(), 2);
        lines.add(worldInfo);

        var loc = player.getLocation();
        var chunk = player.getCurrentChunk();
        var itemInHand = player.getItemInHand();

        var floorLoc = loc.floor(new Vector3f());
        lines.add(
                "ItemInHand:\n§a" + itemInHand.getItemType().getIdentifier().path() + (itemInHand.getMeta() != 0 ? ":" + itemInHand.getMeta() : "") + "\n§f" +
                "StandingOn:\n§a" + player.getBlockStateStandingOn().getBlockType().getIdentifier().path()
        );
        var chunkInfo =
                "Chunk: §a" + chunk.getX() + "," + chunk.getZ() + "\n§f" +
                "Loaded: §a" + player.getDimension().getChunkService().getLoadedChunks().size() + "\n§f" +
                "Loading: §a" + player.getDimension().getChunkService().getLoadingChunks().size() + "\n§f";
        try {
            chunkInfo += "Biome:\n§a" + player.getCurrentChunk().getBiome((int) floorLoc.x() & 15, (int) floorLoc.y(), (int) floorLoc.z() & 15).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            // y coordinate is out of range
            chunkInfo += "Biome: §aN/A";
        }
        lines.add(chunkInfo);

        // Player info
        var playerInfo = "Ping: §a" + player.getPing() + "\n§f" +
                         "Food: §a" + player.getFoodLevel() + "/" + (int) AttributeType.PLAYER_HUNGER.getMaxValue() + "\n§f" +
                         "Exhaustion: §a" + MathUtils.round(player.getFoodExhaustionLevel(), 2) + "/" + (int) AttributeType.PLAYER_EXHAUSTION.getMaxValue() + "\n§f" +
                         "Saturation: §a" + MathUtils.round(player.getFoodSaturationLevel(), 2) + "/" + (int) AttributeType.PLAYER_SATURATION.getMaxValue() + "\n§f" +
                         "Exp: §a" + player.getExperienceInCurrentLevel() + "/" + player.getRequireExperienceForCurrentLevel();
        lines.add(playerInfo);

        scoreboard.setLines(lines);
    }
}