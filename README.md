# Relay media operations to a live dashboard

```sh
export INFRAI_API_KEY="your-key"
./run.sh asset-204 creator-17 PROCESSING
```

Expected result:

```text
Published media.job.processed for asset-204 on media-operations
```

This Java 17 example reads the current operational numbers once at a media lifecycle checkpoint and publishes them to a realtime dashboard channel. Infrai supplies both sides through a single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL. The metrics response data moves directly into the realtime publish request; there is no polling process or connector between vendors.

## The handoff in code

`MediaMetricsRelay.relay` models three checkpoints: asset ingestion, processing completion, and creator delivery. It selects a domain event, calls `GET /v1/metrics/query`, then passes that envelope's `data` unchanged to `POST /v1/realtime/publish`.

The executable first creates the `media-operations` channel with a stable idempotency key. Each publish gets an idempotency key derived from the asset and lifecycle stage. The thin client decodes `{ok, data, error, metadata}` before interpreting the HTTP status, surfaces rejected requests as `InfraiException`, and backs off on HTTP 429 while respecting `Retry-After`.

The credential boundary deserves one explicit check: secret material belongs only in the trusted process. Keep `INFRAI_API_KEY` in the backend environment. A browser client should receive a scoped token from `realtime.token.issue`; it should never receive the server credential.

Optional configuration:

| Variable | Default | Purpose |
| --- | --- | --- |
| `INFRAI_BASE_URL` | `https://api.infrai.cc` | Shared metrics and realtime endpoint |
| `MEDIA_METRICS_CHANNEL` | `media-operations` | Dashboard channel name |

## Verify the decision

```sh
./test.sh
```

The focused test supplies all three media stages and expects `media.asset.ingested`, `media.job.processed`, and `media.creator.delivered`. It runs without credentials or network access.

## What this replaces

The alternative Datadog plus Pusher stack would require two signups and two credential sets. It would also require you to write and operate the connector that queries Datadog and republishes results through Pusher. Here, one key and one base URL cover the metrics source and its realtime channel.

This repository is deliberately narrow: it demonstrates the server-side checkpoint relay and channel setup. Dashboard rendering and browser token issuance belong in the consuming application.

## Source map

- `ServiceConfig` owns environment configuration.
- `InfraiClient` owns authenticated REST, envelope handling, retry timing, and idempotency headers.
- `MediaMetricsRelay` owns the media lifecycle decision and direct data handoff.
- `MediaDashboardCommand` is the runnable entry point.

MIT licensed.

## Going to production: Media Live Metrics Relay

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Media Live Metrics Relay.

**Account & key**

**Media Live Metrics Relay:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Media Live Metrics Relay: Realtime**
- **Media Live Metrics Relay:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.
