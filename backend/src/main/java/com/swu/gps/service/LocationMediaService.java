package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.response.LocationMediaResponse;
import com.swu.gps.repository.GuideLocationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocationMediaService {

    private final GuideLocationRepository guideLocationRepository;
    private final MediaService mediaService;

    public LocationMediaService(GuideLocationRepository guideLocationRepository,
                                MediaService mediaService) {
        this.guideLocationRepository = guideLocationRepository;
        this.mediaService = mediaService;
    }

    @Transactional
    public LocationMediaResponse addImage(Long locationId, MultipartFile file) {
        GuideLocation location = findLocation(locationId);
        String url = mediaService.uploadForLocation(file, "image", location);
        List<String> urls = new ArrayList<>(location.getImageUrls());
        urls.add(url);
        location.setImageUrls(urls);
        location.setUpdatedAt(LocalDateTime.now());
        guideLocationRepository.save(location);
        return toResponse(location);
    }

    @Transactional
    public LocationMediaResponse removeImage(Long locationId, String imageUrl) {
        GuideLocation location = findLocation(locationId);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请指定要删除的图片地址");
        }
        String normalized = imageUrl.trim();
        List<String> urls = new ArrayList<>(location.getImageUrls());
        if (!urls.remove(normalized)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "该图片不在当前讲解点中");
        }
        mediaService.deleteByPublicUrl(normalized);
        location.setImageUrls(urls);
        location.setUpdatedAt(LocalDateTime.now());
        guideLocationRepository.save(location);
        return toResponse(location);
    }

    @Transactional
    public LocationMediaResponse replaceAudio(Long locationId, MultipartFile file) {
        GuideLocation location = findLocation(locationId);
        if (location.getAudioUrl() != null && !location.getAudioUrl().isBlank()) {
            mediaService.deleteByPublicUrl(location.getAudioUrl());
        }
        mediaService.deleteLocationListenFolder(location);
        String url = mediaService.uploadForLocation(file, "audio", location);
        location.setAudioUrl(url);
        location.setUpdatedAt(LocalDateTime.now());
        guideLocationRepository.save(location);
        return toResponse(location);
    }

    @Transactional
    public LocationMediaResponse deleteAudio(Long locationId) {
        GuideLocation location = findLocation(locationId);
        if (location.getAudioUrl() != null && !location.getAudioUrl().isBlank()) {
            mediaService.deleteByPublicUrl(location.getAudioUrl());
            mediaService.deleteLocationListenFolder(location);
            location.setAudioUrl(null);
            location.setUpdatedAt(LocalDateTime.now());
            guideLocationRepository.save(location);
        }
        return toResponse(location);
    }

    @Transactional
    public void deleteAllMediaForLocation(GuideLocation location) {
        for (String url : new ArrayList<>(location.getImageUrls())) {
            mediaService.deleteByPublicUrl(url);
        }
        if (location.getAudioUrl() != null && !location.getAudioUrl().isBlank()) {
            mediaService.deleteByPublicUrl(location.getAudioUrl());
        }
        mediaService.deleteLocationListenFolder(location);
    }

    private GuideLocation findLocation(Long id) {
        return guideLocationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "讲解点不存在: " + id));
    }

    private LocationMediaResponse toResponse(GuideLocation location) {
        return new LocationMediaResponse(location.getImageUrls(), location.getAudioUrl());
    }
}
