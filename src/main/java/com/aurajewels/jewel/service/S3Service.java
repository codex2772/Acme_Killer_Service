/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aurajewels.jewel.service;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Upload a file to S3 and return its public URL.
     *
     * @param file the multipart file to upload
     * @param folder the S3 folder/prefix (e.g. "jewelry-items", "stores")
     * @return the public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            String url =
                    String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

            log.info("Uploaded image to S3: {}", url);
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Delete a file from S3 by its full URL.
     *
     * @param fileUrl the full S3 URL of the file
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // Extract key from URL: https://bucket.s3.region.amazonaws.com/key
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (!fileUrl.startsWith(prefix)) {
                log.warn("Cannot delete file — URL doesn't match bucket: {}", fileUrl);
                return;
            }

            String key = fileUrl.substring(prefix.length());

            DeleteObjectRequest request =
                    DeleteObjectRequest.builder().bucket(bucketName).key(key).build();

            s3Client.deleteObject(request);
            log.info("Deleted image from S3: {}", key);

        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", fileUrl, e);
        }
    }
}
