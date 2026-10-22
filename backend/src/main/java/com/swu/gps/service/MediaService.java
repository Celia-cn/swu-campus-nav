package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.ShareCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class MediaService {

    private static final Set<String> IMAGE_EXT = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));
    private static final Set<String> AUDIO_EXT = new HashSet<>(Arrays.asList("mp3", "wav", "m4a", "aac", "ogg"));

    private final Path mediaRoot;
    private final Path pictureRoot;
    private final Path listenRoot;

    public MediaService(@Value("${app.media.upload-dir:uploads_media}") String uploadDir) {
        this.mediaRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.pictureRoot = mediaRoot.resolve("picture");
        this.listenRoot = mediaRoot.resolve("listen");
    }

    public static String locationFolderKey(GuideLocation location) {
        return "loc_" + location.getId();
    }

    public String uploadForLocation(MultipartFile file, String type, GuideLocation location) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!"image".equals(normalizedType) && !"audio".equals(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type 仅支持 image 或 audio");
        }

        String ext = extractExtension(file.getOriginalFilename());
        Set<String> allowed = "image".equals(normalizedType) ? IMAGE_EXT : AUDIO_EXT;
        if (!allowed.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不支持的文件类型: ." + ext);
        }

        String filename = safeOriginalFilename(file.getOriginalFilename());
        String locationKey = locationFolderKey(location);
        Path targetDir = ("image".equals(normalizedType) ? pictureRoot : listenRoot).resolve(locationKey);

        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();
            if (!target.startsWith(targetDir)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "非法文件名");
            }
            if (Files.exists(target)) {
                throw new ApiException(HttpStatus.CONFLICT, "该景点已存在同名文件：" + filename);
            }
            file.transferTo(target);
            String mediaType = "image".equals(normalizedType) ? "picture" : "listen";
            return "/media/" + mediaType + "/" + locationKey + "/" + filename;
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
    }

    /** 兼容旧通用上传接口（不推荐，仅供过渡）。 */
    public String upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!"image".equals(normalizedType) && !"audio".equals(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type 仅支持 image 或 audio");
        }
        String ext = extractExtension(file.getOriginalFilename());
        Set<String> allowed = "image".equals(normalizedType) ? IMAGE_EXT : AUDIO_EXT;
        if (!allowed.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不支持的文件类型: ." + ext);
        }
        String filename = safeOriginalFilename(file.getOriginalFilename());
        String mediaType = "image".equals(normalizedType) ? "picture" : "listen";
        Path targetDir = ("image".equals(normalizedType) ? pictureRoot : listenRoot).resolve("_common");
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();
            if (!target.startsWith(targetDir)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "非法文件名");
            }
            file.transferTo(target);
            return "/media/" + mediaType + "/_common/" + filename;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
    }

    public void deleteByPublicUrl(String publicUrl) {
        Path file = resolveFilePath(publicUrl);
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

  public void deleteLocationListenFolder(GuideLocation location) {
        Path dir = listenRoot.resolve(locationFolderKey(location)).normalize();
        if (!dir.startsWith(listenRoot) || !Files.isDirectory(dir)) {
            return;
        }
        try {
            Files.list(dir).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public Path resolveFilePath(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String path = publicUrl.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return null;
        }
        if (!path.startsWith("/media/")) {
            return null;
        }
        String[] segments = path.split("/");
        if (segments.length < 5) {
            return resolveLegacyFilePath(segments);
        }
        String category = segments[2];
        String locationKey = segments[3];
        String filename = segments[4];
        if (filename.contains("..") || locationKey.contains("..")) {
            return null;
        }
        Path root;
        if ("picture".equals(category)) {
            root = pictureRoot;
        } else if ("listen".equals(category)) {
            root = listenRoot;
        } else if ("food_user".equals(category) || "hotel_user".equals(category)) {
            root = mediaRoot.resolve(category);
        } else {
            return resolveLegacyFilePath(segments);
        }
        Path file = root.resolve(locationKey).resolve(filename).normalize();
        Path base = root.resolve(locationKey).normalize();
        if (!file.startsWith(base)) {
            return null;
        }
        return file;
    }

    private Path resolveLegacyFilePath(String[] segments) {
        if (segments.length < 4) {
            return null;
        }
        String legacyType = segments[2];
        String filename = segments[3];
        if (filename.contains("..")) {
            return null;
        }
        if ("image".equals(legacyType)) {
            Path file = mediaRoot.resolve("media").resolve("image").resolve(filename).normalize();
            Path root = mediaRoot.resolve("media").resolve("image").normalize();
            return file.startsWith(root) ? file : null;
        }
        if ("audio".equals(legacyType)) {
            Path file = mediaRoot.resolve("media").resolve("audio").resolve(filename).normalize();
            Path root = mediaRoot.resolve("media").resolve("audio").normalize();
            return file.startsWith(root) ? file : null;
        }
        return null;
    }

    public Path getPictureRoot() {
        return pictureRoot;
    }

    public Path getListenRoot() {
        return listenRoot;
    }

    public Path getMediaRoot() {
        return mediaRoot;
    }

  public Path getFoodUserRoot() {
        return mediaRoot.resolve("food_user");
    }

    public Path getHotelUserRoot() {
        return mediaRoot.resolve("hotel_user");
    }

    public String uploadSharePostImage(Long userId, ShareCategory category, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (!IMAGE_EXT.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不支持的图片类型: ." + ext);
        }
        String folder = category == ShareCategory.FOOD ? "food_user" : "hotel_user";
        String filename = safeOriginalFilename(file.getOriginalFilename());
        Path targetDir = mediaRoot.resolve(folder).resolve(String.valueOf(userId));
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();
            if (!target.startsWith(targetDir)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "非法文件名");
            }
            if (Files.exists(target)) {
                throw new ApiException(HttpStatus.CONFLICT, "已存在同名图片：" + filename);
            }
            file.transferTo(target);
            return "/media/" + folder + "/" + userId + "/" + filename;
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }
    }

    public String saveShareTitleImage(Long userId, ShareCategory category, String title, byte[] pngBytes) {
        String folder = category == ShareCategory.FOOD ? "food_user" : "hotel_user";
        String filename = "title_" + System.currentTimeMillis() + ".png";
        Path targetDir = mediaRoot.resolve(folder).resolve(String.valueOf(userId));
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();
            Files.write(target, pngBytes);
            return "/media/" + folder + "/" + userId + "/" + filename;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "标题图片生成失败");
        }
    }

    private String safeOriginalFilename(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String name = Path.of(original).getFileName().toString().trim();
        name = name.replace("\\", "_").replace("/", "_");
        name = name.replaceAll("[<>:\"|?*]", "_");
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return "file";
        }
        return name;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
