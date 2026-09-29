package dev.infrai.media;

import dev.infrai.media.MediaMetricsRelay.AssetCheckpoint;
import dev.infrai.media.MediaMetricsRelay.RelayReceipt;
import dev.infrai.media.MediaMetricsRelay.Stage;

public final class MediaDashboardCommand {
    private MediaDashboardCommand() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: MediaDashboardCommand <asset-id> <creator-account-id> <INGESTION|PROCESSING|DELIVERY>");
            System.exit(2);
        }
        ServiceConfig config = ServiceConfig.fromEnvironment();
        MediaMetricsRelay relay = new MediaMetricsRelay(new InfraiClient(config), config.channel());
        RelayReceipt receipt = relay.relay(new AssetCheckpoint(args[0], args[1], Stage.valueOf(args[2])));
        System.out.println("Published " + receipt.event() + " for " + receipt.assetId()
                + " on " + receipt.channel());
    }
}
