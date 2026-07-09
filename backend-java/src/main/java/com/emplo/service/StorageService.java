package com.emplo.service;

import com.emplo.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Uploads files to Supabase Storage and returns public URLs.
 *
 * Supabase Storage REST API:
 * POST /storage/v1/object/{bucket}/{path} — upload file
 * GET  /storage/v1/object/public/{bucket}/{path} — public URL
 */
@Service
@Slf4j
public class StorageService {

    @Value("${app.supabase.url:}")
    private String supabaseUrl;

    @Value("${app.supabase.service-key:}")
    private String supabaseServiceKey;

    @Value("${app.supabase.storage-bucket:uploads}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Upload a file to Supabase Storage.
     * Returns the public URL of the uploaded file.
     */
    public String upload(MultipartFile file, String folder) {
        if (supabaseUrl.isBlank() || supabaseServiceKey.isBlank()) {
            // Fallback to base64 if Supabase not configured
            return uploadAsBase64(file);
        }

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("File too large. Maximum 10MB.");
        }

        try {
            // Generate a unique path: folder/uuid-originalname
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "file";
            String path = folder + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + originalName;

            // Upload to Supabase Storage
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceKey);
            headers.set("apikey", supabaseServiceKey);
            headers.setContentType(MediaType.valueOf(
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream"
            ));
            headers.set("x-upsert", "true"); // overwrite if exists

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, entity, String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase upload failed: {}", response.getBody());
                throw new BadRequestException("Failed to upload file to storage");
            }

            // Return the public URL
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Storage upload error: {}", e.getMessage());
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Upload raw bytes to Supabase Storage (used for generated thumbnails).
     */
    public String uploadBytes(byte[] bytes, String contentType, String filename, String folder) {
        if (supabaseUrl.isBlank() || supabaseServiceKey.isBlank()) {
            // Fallback: return base64
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            return "data:" + contentType + ";base64," + b64;
        }

        try {
            String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String path = folder + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceKey);
            headers.set("apikey", supabaseServiceKey);
            headers.setContentType(MediaType.valueOf(contentType));
            headers.set("x-upsert", "true");

            HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BadRequestException("Failed to upload thumbnail");
            }

            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Thumbnail upload error: {}", e.getMessage());
            // Return base64 fallback
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            return "data:" + contentType + ";base64," + b64;
        }
    }

    /**
     * Delete a file from Supabase Storage by its public URL.
     */
    public void delete(String publicUrl) {
        if (supabaseUrl.isBlank() || supabaseServiceKey.isBlank()) return;
        if (publicUrl == null || !publicUrl.contains("/storage/v1/object/public/")) return;

        try {
            // Extract path from public URL
            String prefix = "/storage/v1/object/public/" + bucket + "/";
            int idx = publicUrl.indexOf(prefix);
            if (idx < 0) return;
            String path = publicUrl.substring(idx + prefix.length());

            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceKey);
            headers.set("apikey", supabaseServiceKey);

            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage: {}", e.getMessage());
        }
    }

    /**
     * Fallback: convert file to base64 data URL (used when Supabase is not configured).
     */
    private String uploadAsBase64(MultipartFile file) {
        try {
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new BadRequestException("File too large. Maximum 5MB.");
            }
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String base64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            return "data:" + contentType + ";base64," + base64;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to read file");
        }
    }
}
