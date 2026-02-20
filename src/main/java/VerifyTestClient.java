/**
 * IDVerse Verify Test Client
 *
 * Demonstrates a full dry-run verification flow:
 *   1. POST /api/verify/test?dryRun=true          — submits a verification without sending a real SMS
 *   2. GET  /api/status/reference/{referenceId}   — polls for the initial status
 *   3. POST /api/updateStatus  (event=pending)    — simulates link sent to end-user
 *   4. POST /api/updateStatus  (event=liveness)   — simulates end-user at liveness phase
 *   5. POST /api/updateStatus  (event=completedPass) — simulates a passing verification
 *   6. GET  /api/status/transaction/{transactionId}  — polls for the final status
 *
 * Usage:
 *   java VerifyTestClient.java
 *
 * Requirements: Java 11+ (uses java.net.http.HttpClient — no external dependencies)
 *
 * To run against a live instance, update the constants below.
 */

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class VerifyTestClient {

    // -------------------------------------------------------------------------
    // Configuration — edit these values before running
    // -------------------------------------------------------------------------
    static final String BASE_URL     = "http://localhost:19746";
    static final String PHONE_CODE   = "+1";
    static final String PHONE_NUMBER = "9412607454";
    static final String REFERENCE_ID = "REF-EXAMPLE-001";
    static final String TRANSACTION_ID = "txn-example-0000001"; // must be 10-128 chars, alphanumeric/-/_
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Step 1: POST /api/verify/test?dryRun=true
        System.out.println("=== Step 1: Submit dry-run verification ===");
        String verifyResponse = postVerifyTest(client);
        System.out.println("Response: " + verifyResponse);

        // Step 2: GET /api/status/reference/{referenceId}
        System.out.println("\n=== Step 2: Poll status by reference ID ===");
        String statusResponse = getStatusByReference(client);
        System.out.println("Response: " + statusResponse);

        // Step 3: Simulate pending (link sent to end-user)
        System.out.println("\n=== Step 3: Update status — pending ===");
        String pendingResponse = updateStatus(client, "pending");
        System.out.println("Response: " + pendingResponse);

        // Step 4: Simulate liveness (end-user at liveness phase)
        System.out.println("\n=== Step 4: Update status — liveness ===");
        String livenessResponse = updateStatus(client, "liveness");
        System.out.println("Response: " + livenessResponse);

        // Step 5: Simulate completedPass (verification passed)
        System.out.println("\n=== Step 5: Update status — completedPass ===");
        String completedResponse = updateStatus(client, "completedPass");
        System.out.println("Response: " + completedResponse);

        // Step 6: GET /api/status/transaction/{transactionId} — confirm final status
        System.out.println("\n=== Step 6: Poll final status by transaction ID ===");
        String finalStatus = getStatusByTransaction(client);
        System.out.println("Response: " + finalStatus);
    }

    static String postVerifyTest(HttpClient client) throws Exception {
        String url = BASE_URL + "/api/verify/test?dryRun=true";

        String body = String.format(
                "{\"phoneCode\":\"%s\",\"phoneNumber\":\"%s\",\"referenceId\":\"%s\",\"transactionId\":\"%s\"}",
                PHONE_CODE, PHONE_NUMBER, REFERENCE_ID, TRANSACTION_ID
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println("POST " + url);
        System.out.println("Body: " + body);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Unexpected status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    static String updateStatus(HttpClient client, String event) throws Exception {
        String url = BASE_URL + "/api/updateStatus";

        String body = String.format(
                "{\"transactionId\":\"%s\",\"event\":\"%s\"}",
                TRANSACTION_ID, event
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println("POST " + url);
        System.out.println("Body: " + body);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Unexpected status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    static String getStatusByReference(HttpClient client) throws Exception {
        String url = BASE_URL + "/api/status/reference/" + REFERENCE_ID;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        System.out.println("GET " + url);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        if (response.statusCode() == 404) {
            throw new RuntimeException("No record found for referenceId: " + REFERENCE_ID);
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Unexpected status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    static String getStatusByTransaction(HttpClient client) throws Exception {
        String url = BASE_URL + "/api/status/transaction/" + TRANSACTION_ID;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        System.out.println("GET " + url);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        if (response.statusCode() == 404) {
            throw new RuntimeException("No record found for transactionId: " + TRANSACTION_ID);
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Unexpected status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }
}
