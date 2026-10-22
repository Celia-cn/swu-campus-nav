package com.swu.gps.config;

import com.swu.gps.security.AuthInterceptor;
import com.swu.gps.service.MediaService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final MediaService mediaService;

    public WebConfig(AuthInterceptor authInterceptor, MediaService mediaService) {
        this.authInterceptor = authInterceptor;
        this.mediaService = mediaService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/picture/**")
                .addResourceLocations(asFileLocation(mediaService.getPictureRoot()));
        registry.addResourceHandler("/media/listen/**")
                .addResourceLocations(asFileLocation(mediaService.getListenRoot()));
        registry.addResourceHandler("/media/image/**")
                .addResourceLocations(asFileLocation(mediaService.getMediaRoot().resolve("media").resolve("image")));
        registry.addResourceHandler("/media/audio/**")
                .addResourceLocations(asFileLocation(mediaService.getMediaRoot().resolve("media").resolve("audio")));
    }

    private static String asFileLocation(Path dir) {
        String location = dir.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
