package sh.skills.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Anonymous usage telemetry (opt-out).
 * Mirrors telemetry.ts from the TypeScript source.
 * Disabled by setting DO_NOT_TRACK=1 or SKILLS_TELEMETRY=0.
 */
public class Telemetry {

    private static final boolean ENABLED = isTelemetryEnabled();
    private static final String ENDPOINT = "https://skills.sh/api/telemetry";

    private static boolean isTelemetryEnabled() {
        // Respect DO_NOT_TRACK (https://consoledonottrack.com/)
        String dnt = System.getenv("DO_NOT_TRACK");
        if (dnt != null && !dnt.equals("0")) return false;

        String skillsTelemetry = System.getenv("SKILLS_TELEMETRY");
        if ("0".equals(skillsTelemetry) || "false".equals(skillsTelemetry)) return false;

        // Disable in CI by default
        if (System.getenv("CI") != null) return false;

        return true;
    }

    /**
     * Send a telemetry event asynchronously (fire-and-forget).
     * Never blocks the main flow.
     */
    public static void track(String event, String data) {
        if (!ENABLED) return;

        String payload = "{\"event\":\"" + escape(event) + "\",\"data\":\"" + escape(data) + "\"}";

        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .timeout(Duration.ofSeconds(5))
                        .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
                // Telemetry failures are always silently ignored
            }
        });
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
