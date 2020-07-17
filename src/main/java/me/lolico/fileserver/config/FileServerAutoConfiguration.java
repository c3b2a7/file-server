package me.lolico.fileserver.config;

import me.lolico.fileserver.config.prop.FileServerProperty;
import me.lolico.fileserver.util.FileUtils;
import me.lolico.fileserver.web.UploadController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * @author lolico
 */
@Configuration
@EnableConfigurationProperties(FileServerProperty.class)
@ConditionalOnProperty(prefix = "me.lolico.file-server", name = "enabled", matchIfMissing = true)
public class FileServerAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(FileServerAutoConfiguration.class);
    private final FileServerProperty property;

    public FileServerAutoConfiguration(FileServerProperty property) {
        this.property = property;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                if (property.getBasePath() != null) {
                    registry.addResourceHandler(resolvePathPattern(property.getBasePath()))
                            .addResourceLocations(property.getUploadDirectory().toURI().toString());
                }
            }

            private String resolvePathPattern(String basePath) {
                if (!basePath.startsWith("/")) {
                    basePath = "/" + basePath;
                }
                if (!basePath.endsWith("**")) {
                    basePath = basePath.endsWith("/") ? basePath + "**" : basePath + "/**";
                }
                return basePath;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public FileUtils fileUtils() {
        File uploadDirectory = property.getUploadDirectory();
        // Directory name is used by default
        String basePath = resolveSpecifiedBasePath(property.getBasePath(), uploadDirectory.getName());
        return new FileUtils(uploadDirectory, property.getAllowFilePattern(), basePath, property.getExpandDirectory());
    }

    private String resolveSpecifiedBasePath(String path, String defaultPath) {
        return StringUtils.hasText(path) ? path : defaultPath;
    }

    @Bean
    @ConditionalOnMissingBean
    public UploadController uploadController(FileUtils fileUtils) {
        UploadController controller = new UploadController(fileUtils);
        logger.info("Enabled file server, expose EndPoint to '/uploads'");
        return controller;
    }

}
