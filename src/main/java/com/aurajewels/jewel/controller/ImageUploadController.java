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
package com.aurajewels.jewel.controller;

import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.S3Service;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final String[] ALLOWED_TYPES = {
        "image/jpeg", "image/png", "image/webp", "image/gif"
    };

    private final S3Service s3Service;

    /** Upload an image for a jewelry item. Returns the public S3 URL. */
    @PostMapping("/upload")
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "jewelry-items") String folder) {

        // Validate file is not empty
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5MB limit"));
        }

        // Validate content type
        String contentType = file.getContentType();
        boolean isAllowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only JPEG, PNG, WebP, and GIF images are allowed"));
        }

        // Sanitize folder name — only allow alphanumeric, hyphens, underscores
        String sanitizedFolder = folder.replaceAll("[^a-zA-Z0-9\\-_]", "");
        if (sanitizedFolder.isBlank()) {
            sanitizedFolder = "jewelry-items";
        }

        String imageUrl = s3Service.uploadFile(file, sanitizedFolder);

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    /** Delete an image from S3. */
    @DeleteMapping
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<?> deleteImage(@RequestParam("url") String imageUrl) {
        s3Service.deleteFile(imageUrl);
        return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
    }
}
