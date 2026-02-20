/**
 * IDVerse Verify Test Client
 *
 * Demonstrates a full dry-run verification flow:
 *   1. POST /api/verify/test?dryRun=true  — submits a verification without sending a real SMS
 *   2. GET  /api/status/reference/{id}    — polls for the latest status of that reference ID
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
}
