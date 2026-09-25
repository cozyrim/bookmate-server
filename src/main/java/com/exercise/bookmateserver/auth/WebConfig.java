package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.ratelimit.UserWriteLimitInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final UserWriteLimitInterceptor userWriteLimitInterceptor;
    private final Path profileImageUploadDirectory;

    public WebConfig(
            AuthInterceptor authInterceptor,
            UserWriteLimitInterceptor userWriteLimitInterceptor,
            @Value("${app.upload.profile-image-dir:uploads/profile-images}") String profileImageUploadDirectory
    ) {
        this.authInterceptor = authInterceptor;
        this.userWriteLimitInterceptor = userWriteLimitInterceptor;
        this.profileImageUploadDirectory = Paths.get(profileImageUploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
        registry.addInterceptor(userWriteLimitInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profile-images/**")
                .addResourceLocations(profileImageUploadDirectory.toUri().toString());
    }
}
