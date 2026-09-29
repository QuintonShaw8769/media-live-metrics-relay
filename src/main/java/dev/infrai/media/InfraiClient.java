package dev.infrai.media;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiClient {
    private static final int MAX_ATTEMPTS = 4;
    private final ServiceConfig config;
    private final HttpClient http;

    public InfraiClient(ServiceConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build());
    }

    InfraiClient(ServiceConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public Object queryMetrics(String name, String agg) throws IOException, InterruptedException {
        String query = "?name=" + encode(name) + "&agg=" + encode(agg);
        return request("GET", "/v1/metrics/query" + query, null, null);
    }

    public Object createChannel(String channel, String operationId) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", channel);
        body.put("type", "public");
        body.put("vendor", "infrai");
        return request("POST", "/v1/realtime/channel/create", body, operationId);
    }

    public Object publish(String channel, String event, Object data, String accountId, String operationId)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", channel);
        body.put("event", event);
        body.put("data", data);
        body.put("account_id", accountId);
        return request("POST", "/v1/realtime/publish", body, operationId);
    }

    private Object request(String method, String path, Object body, String idempotencyKey)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                    .timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(Json.write(body)));

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.decode(response.body(), response.statusCode());
            if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (!envelope.ok()) throw new InfraiException(envelope.error(), response.statusCode());
            if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
            return envelope.data();
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private URI resolve(String path) {
        return URI.create(config.baseUrl().toString().replaceAll("/$", "") + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String header = response.headers().firstValue("Retry-After").orElse("");
        try { return Duration.ofSeconds(Math.max(0, Long.parseLong(header))); }
        catch (NumberFormatException ignored) { return Duration.ofMillis(250L * (1L << attempt)); }
    }

    private record Envelope(boolean ok, Object data, Map<String, Object> error) {
        @SuppressWarnings("unchecked")
        static Envelope decode(String body, int status) throws IOException {
            final Object decoded;
            try { decoded = Json.parse(body); }
            catch (RuntimeException e) { throw new IOException("Invalid JSON response at status " + status, e); }
            if (!(decoded instanceof Map<?, ?> raw)) throw new IOException("Response envelope must be an object");
            Map<String, Object> map = (Map<String, Object>) raw;
            boolean ok = Boolean.TRUE.equals(map.get("ok"));
            Map<String, Object> error = map.get("error") instanceof Map<?, ?> value
                    ? (Map<String, Object>) value : Map.of("message", "Request rejected");
            return new Envelope(ok, map.get("data"), error);
        }
    }

    public static final class InfraiException extends IOException {
        private final Map<String, Object> error;
        private final int status;

        InfraiException(Map<String, Object> error, int status) {
            super(String.valueOf(error.getOrDefault("message", "Infrai request rejected")));
            this.error = Map.copyOf(error);
            this.status = status;
        }

        public Map<String, Object> error() { return error; }
        public int status() { return status; }
    }
}
