package dev.infrai.media;

import dev.infrai.media.MediaMetricsRelay.Stage;

public final class MediaMetricsRelayTest {
    public static void main(String[] args) {
        assertEquals("media.asset.ingested", MediaMetricsRelay.eventFor(Stage.INGESTION));
        assertEquals("media.job.processed", MediaMetricsRelay.eventFor(Stage.PROCESSING));
        assertEquals("media.creator.delivered", MediaMetricsRelay.eventFor(Stage.DELIVERY));
        System.out.println("MediaMetricsRelayTest passed: all lifecycle decisions are stable");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + ", got " + actual);
    }
}
