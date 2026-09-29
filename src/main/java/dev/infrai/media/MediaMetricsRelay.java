package dev.infrai.media;

import java.io.IOException;

public final class MediaMetricsRelay {
    private static final String METRIC_NAME = "media.operations";
    private static final String METRIC_AGG = "sum";

    public enum Stage { INGESTION, PROCESSING, DELIVERY }

    public record AssetCheckpoint(String assetId, String creatorAccountId, Stage stage) {
        public AssetCheckpoint {
            if (assetId == null || assetId.isBlank()) throw new IllegalArgumentException("assetId is required");
            if (creatorAccountId == null || creatorAccountId.isBlank()) {
                throw new IllegalArgumentException("creatorAccountId is required");
            }
        }
    }

    public record RelayReceipt(String channel, String event, String assetId) {}

    private final InfraiClient infrai;
    private final String channel;

    public MediaMetricsRelay(InfraiClient infrai, String channel) {
        this.infrai = infrai;
        this.channel = channel;
    }

    public void ensureChannel() throws IOException, InterruptedException {
        infrai.createChannel(channel, "channel:" + channel);
    }

    public RelayReceipt relay(AssetCheckpoint checkpoint) throws IOException, InterruptedException {
        String event = eventFor(checkpoint.stage());
        Object metrics = infrai.queryMetrics(METRIC_NAME, METRIC_AGG);
        infrai.publish(channel, event, metrics, checkpoint.creatorAccountId(),
                "asset:" + checkpoint.assetId() + ":" + checkpoint.stage().name());
        return new RelayReceipt(channel, event, checkpoint.assetId());
    }

    static String eventFor(Stage stage) {
        return switch (stage) {
            case INGESTION -> "media.asset.ingested";
            case PROCESSING -> "media.job.processed";
            case DELIVERY -> "media.creator.delivered";
        };
    }
}
