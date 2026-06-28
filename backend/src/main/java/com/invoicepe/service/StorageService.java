package com.invoicepe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Uploads invoice PDFs to Supabase Storage and returns a public URL.
 * When storage is not configured (dev placeholders), uploads are skipped and
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

    public String uploadInvoicePdf(String invoiceNumber, byte[] pdf) {
        if (!isConfigured()) {
            log.warn("Supabase storage not configured; skipping PDF upload for invoice {}", invoiceNumber);
            return null;
        }

        String objectPath = "invoices/" + invoiceNumber + ".pdf";
        String base = trimTrailingSlash(supabaseUrl);
        String endpoint = base + "/storage/v1/object/" + bucket + "/" + objectPath;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("Content-Type", "application/pdf")
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(pdf))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 == 2) {
                String publicUrl = base + "/storage/v1/object/public/" + bucket + "/" + objectPath;
                log.info("Uploaded invoice {} PDF to {}", invoiceNumber, publicUrl);
                return publicUrl;
            }
            log.error("Supabase upload failed ({}): {}", response.statusCode(), response.body());
            return null;
        } catch (Exception e) {
            log.error("Supabase upload error for invoice {}", invoiceNumber, e);
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
