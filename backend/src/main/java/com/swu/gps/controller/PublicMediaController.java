package com.swu.gps.controller;

import com.swu.gps.service.MediaService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class PublicMediaController {

    private final MediaService mediaService;

    public PublicMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/media/picture/{locationKey}/{filename:.+}")
    public ResponseEntity<Resource> picture(@PathVariable String locationKey,
                                            @PathVariable String filename) {
        return serve(mediaService.getPictureRoot(), locationKey, filename);
    }

    @GetMapping("/media/listen/{locationKey}/{filename:.+}")
    public ResponseEntity<Resource> listen(@PathVariable String locationKey,
                                         @PathVariable String filename) {
        return serve(mediaService.getListenRoot(), locationKey, filename);
    }

    @GetMapping("/media/food_user/{userId}/{filename:.+}")
    public ResponseEntity<Resource> foodUser(@PathVariable String userId,
                                             @PathVariable String filename) {
        return serve(mediaService.getFoodUserRoot(), userId, filename);
    }

    @GetMapping("/media/hotel_user/{userId}/{filename:.+}")
    public ResponseEntity<Resource> hotelUser(@PathVariable String userId,
                                            @PathVariable String filename) {
        return serve(mediaService.getHotelUserRoot(), userId, filename);
    }

    /** 兼容旧路径 /media/image/{file} */
    @GetMapping("/media/image/{filename:.+}")
    public ResponseEntity<Resource> legacyImage(@PathVariable String filename) {
        return serveFlat(mediaService.getMediaRoot().resolve("media").resolve("image"), filename);
    }

    @GetMapping("/media/audio/{filename:.+}")
    public ResponseEntity<Resource> legacyAudio(@PathVariable String filename) {
        return serveFlat(mediaService.getMediaRoot().resolve("media").resolve("audio"), filename);
    }

    private ResponseEntity<Resource> serve(Path root, String locationKey, String filename) {
        if (locationKey == null || locationKey.contains("..") || filename == null || filename.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        Path file = root.resolve(locationKey).resolve(filename).normalize();
        Path base = root.resolve(locationKey).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        return build(file);
    }

    private ResponseEntity<Resource> serveFlat(Path root, String filename) {
        if (filename == null || filename.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        return build(file);
    }

    private ResponseEntity<Resource> build(Path file) {
        FileSystemResource resource = new FileSystemResource(file);
        MediaType type = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(type)
                .body(resource);
    }
}
