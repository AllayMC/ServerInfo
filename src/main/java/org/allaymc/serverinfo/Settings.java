package org.allaymc.serverinfo;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author daoge_cmd
 */
@Getter
@Accessors(fluent = true)
public class Settings extends OkaeriConfig {

    @CustomKey("show-world-info")
    private boolean showWorldInfo = true;

    @CustomKey("show-player-info")
    private boolean showPlayerInfo = true;

    @CustomKey("show-chunk-info")
    private boolean showChunkInfo = true;

    @CustomKey("show-light-info")
    private boolean showLightInfo = true;

    @CustomKey("show-misc-info")
    private boolean showMiscInfo = true;

    @CustomKey("show-mspt-bar")
    private boolean showMSPTBar = true;
}
