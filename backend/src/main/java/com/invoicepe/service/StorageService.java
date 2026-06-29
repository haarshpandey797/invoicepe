package com.invoicepe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Uploads invoice PDFs to a PRIVATE Supabase Storage bucket and returns a
 * time-limited signed URL.
 *
 * <p>The bucket must NOT be public. Invoices carry sensitive data (GSTIN,
 * amounts, client + bank details) and the object path is the invoice number,
 * which is sequential and therefore guessable. A public bucket would let anyone
 * enumerate every user's invoices. Instead the bucket stays private and we ask
 * Supabase to mint a signed URL: an unguessable, expiring token that lets the
 * client open the PDF from WhatsApp/email without exposing the bucket.
 *
 * <p>When storage is not configured (dev placeholders), uploads are skipped and
 * null is returned so the calling flow can continue.
 */
@Service
@Slf4j
public class StorageService {

    @Value("${app.storage.supabase-url}")
    private String supabaseUrl;

    @Value("${app.storage.supabase-key}")
    private String supabaseKey;

    @Value("${app.storage.bucket-name}")
    private String bucket;

    /** How long a signed invoice-PDF URL stays valid, in seconds. Default 90 days. */
    @Value("${app.storage.signed-url-expiry-seconds:7776000}")
    private long signedUrlExpirySeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String uploadInvoicePdf(String invoiceNumber, byte[] pdf) {
        if (!isConfigured()) {
            log.warn("Supabase storage not configured; skipping PDF upload for invoice {}", invoiceNumber);
            return null;
        }

        String objectPath = "invoices/" + invoiceNumber + ".pdf";
        String base = trimTrailingSlash(supabaseUrl);

        if (!upload(base, objectPath, pdf, invoiceNumber)) {
            return null;
        }
        return createSignedUrl(base, objectPath, invoiceNumber);
    }

    private boolean upload(String base, String objectPath, byte[] pdf, String invoiceNumber) {
        String endpoint = base + "/storage/v1/object/" + bucket + "/" + objectPath;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("Content-Type", "application/pdf")
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(pdf))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                log.info("Uploaded invoice {} PDF to private bucket", invoiceNumber);
                return true;
            }
            log.error("Supabase upload failed ({}): {}", response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.error("Supabase upload error for invoice {}", invoiceNumber, e);
            return false;
        }
    }

    /**
     * Asks Supabase to sign the object so it can be read via a temporary,
     * unguessable URL without the bucket being public.
     */
    private String createSignedUrl(String base, String objectPath, String invoiceNumber) {
        String endpoint = base + "/storage/v1/object/sign/" + bucket + "/" + objectPath;
        String body = "{\"expiresIn\":" + signedUrlExpirySeconds + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                // Supabase returns { "signedURL": "/object/sign/<bucket>/<path>?token=..." }
                JsonNode node = objectMapper.readTree(response.body());
                String signedPath = node.path("signedURL").asText(null);
                if (signedPath == null || signedPath.isBlank()) {
                    log.error("Supabase sign returned no signedURL for invoice {}: {}", invoiceNumber, response.body());
                    return null;
                }
                String signedUrl = base + "/storage/v1" + signedPath;
                log.info("Created signed URL for invoice {} (expires in {}s)", invoiceNumber, signedUrlExpirySeconds);
                return signedUrl;
            }
            log.error("Supabase sign failed ({}): {}", response.statusCode(), response.body());
            return null;
        } catch (Exception e) {
            log.error("Supabase sign error for invoice {}", invoiceNumber, e);
            return null;
        }
    }

    private boolean isConfigured() {
        return isReal(supabaseUrl) && isReal(supabaseKey) && isReal(bucket)
                && !supabaseUrl.contains("localhost")
                && !supabaseUrl.contains("xxxx");
    }

    private boolean isReal(String value) {
        return value != null && !value.isBlank()
                && !value.equalsIgnoreCase("REPLACE_ME")
                && !value.equalsIgnoreCase("placeholder");
    }

    private String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
