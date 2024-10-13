package org.allaymc.serverinfo;

import org.allaymc.api.command.SenderType;
import org.allaymc.api.command.SimpleCommand;
import org.allaymc.api.command.tree.CommandTree;

/**
 * @author daoge_cmd
 */
public class ServerInfoCommand extends SimpleCommand {
    public ServerInfoCommand() {
        super("serverinfo", "Control the server info");
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .key("msptbar")
                .bool("show")
                .exec((context, player) -> {
                    boolean show = context.getResult(1);
                    var bossBar = ServerInfo.INSTANCE.getOrCreateWorldMSPTBar(player.getWorld());
                    if (show) {
                        bossBar.addViewer(player);
                    } else {
                        bossBar.removeViewer(player);
                    }
                    return context.success();
                }, SenderType.PLAYER)
                .root()
                .key("scoreboard")
                .bool("show")
                .exec((context, player) -> {
                    boolean show = context.getResult(1);
                    ServerInfo.INSTANCE.setScoreboardDisabled(player, !show);
                    return context.success();
                }, SenderType.PLAYER);

    }
}
