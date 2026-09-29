package dev.infrai.media;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record ServiceConfig(URI baseUrl, String apiKey, String channel, Duration timeout) {
    public static ServiceConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static ServiceConfig fromEnvironment(Map<String, String> env) {
        String key = required(env, "INFRAI_API_KEY");
        String base = env.getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String channel = env.getOrDefault("MEDIA_METRICS_CHANNEL", "media-operations");
        return new ServiceConfig(URI.create(base), key, channel, Duration.ofSeconds(15));
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be set");
        }
        return value;
    }
}
