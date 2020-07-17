package me.lolico.fileserver.config.prop;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author lolico
 */
@ConfigurationProperties("me.lolico.file-server")
public class FileServerProperty implements InitializingBean {
    /**
     * Whether to enable file upload
     */
    private boolean enabled = true;
    /**
     * The base directory for uploading files
     */
    private File uploadDirectory = new File("uploads");
    /**
     * Pattern to allow file uploading
     */
    private String allowFilePattern = "*.*";
    /**
     * Base path to be used by file server to expose resources.
     */
    private String basePath;
    /**
     * Whether to expand the directory based on current time
     */
    private boolean expandDirectory;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public File getUploadDirectory() {
        return uploadDirectory;
    }

    public void setUploadDirectory(File uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    public String getAllowFilePattern() {
        return allowFilePattern;
    }

    public void setAllowFilePattern(String allowFilePattern) {
        this.allowFilePattern = allowFilePattern;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean getExpandDirectory() {
        return expandDirectory;
    }

    public void setExpandDirectory(boolean expandDirectory) {
        this.expandDirectory = expandDirectory;
    }

    @Override
    public void afterPropertiesSet() {
        if (!uploadDirectory.exists()) {
            Assert.isTrue(uploadDirectory.mkdirs(), "Cannot mkdir '" + uploadDirectory.getAbsolutePath() + "'");
        }
    }
}
