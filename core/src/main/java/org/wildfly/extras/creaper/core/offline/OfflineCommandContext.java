package org.wildfly.extras.creaper.core.offline;

import org.wildfly.extras.creaper.core.ServerVersion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class OfflineCommandContext {
    public final OfflineManagementClient client;
    public final OfflineOptions options; // same as client.options()
    public final ServerVersion version;
    public final File configurationFile; // same as client.options().configurationFile
    public final Map<String, NameSpaceVersion> subsystemVersions;

    OfflineCommandContext(OfflineManagementClient client, ServerVersion version) {
        this(client, version, new HashMap<>());
    }

    OfflineCommandContext(OfflineManagementClient client, ServerVersion version,
                          Map<String, NameSpaceVersion> subsystemVersions) {
        this.client = client;
        this.options = client.options();
        this.version = version;
        this.configurationFile = client.options().configurationFile;
        this.subsystemVersions = subsystemVersions;
    }
}
