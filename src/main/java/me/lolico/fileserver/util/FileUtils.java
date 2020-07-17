package me.lolico.fileserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author lolico
 */
public final class FileUtils {
    private final static Logger log = LoggerFactory.getLogger(FileUtils.class);
    private final File baseDirectory;
    private final String pattern;
    private final String basePath;
    private final boolean expandDirectory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public FileUtils(File baseDirectory, String pattern, String basePath, boolean expandDirectory) {
        this.baseDirectory = baseDirectory.isDirectory() ? baseDirectory : baseDirectory.getParentFile();
        this.pattern = resolvePattern(pattern);
        this.basePath = resolveBasePath(basePath);
        this.expandDirectory = expandDirectory;
    }

    private String resolvePattern(String pattern) {
        if (!pattern.startsWith("^")) {
            pattern = "^" + pattern;
        }
        return pattern;
    }

    private String resolveBasePath(String basePath) {
        for (int i = 0; i < basePath.length(); i++) {
            char ch = basePath.charAt(i);
            Assert.isTrue(isValidChar(ch), "basePath include invalid character");
        }
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        return basePath;
    }

    private boolean isValidChar(char ch) {
        return "!@#$%^&*()_+".indexOf(ch) == -1;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    @Nullable
    public String upload(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        if (match(filename)) {
            UUID uuid = UUID.nameUUIDFromBytes(file.getBytes());

            String prefix = uuid.toString().replace("-", "");
            String suffix = resolveFileExt(filename);
            String name = prefix + suffix;

            File uploadFile;
            if (expandDirectory) {
                uploadFile = getFile(name, LocalDate.now());
            } else {
                uploadFile = getFile(name);
            }
            if (uploadFile.getParentFile().exists()) {
                if (!uploadFile.exists()) {
                    file.transferTo(uploadFile.toPath());
                }
                return resolveFileLink(uploadFile);
            }
            log.error("Directory '{}' is not exist, cancel upload {}", uploadFile.getParent(), filename);
        }
        return null;
    }

    public File getFile(String filename) {
        return new File(baseDirectory, filename);
    }

    public File getFile(String filename, @NonNull LocalDate localDate) {
        File directory = expandDirectory(localDate);
        return new File(directory, filename);
    }

    private File expandDirectory(LocalDate localDate) {
        File dir = new File(baseDirectory, localDate.format(formatter));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error("Cannot mkdir '{}'", dir.getPath());
            }
        }
        return dir;
    }

    public boolean deleteFile(String filename) {
        File file = getFile(filename);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }

    public boolean deleteFile(String filename, @NonNull LocalDate localDate) {
        File file = getFile(filename, localDate);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }

    public Map<String, String> getAllFile() throws IOException {
        return getAllFile(baseDirectory);
    }

    public Map<String, String> getAllFile(File dir) throws IOException {
        if (dir.exists() && dir.isDirectory()) {
            Stream<Path> list = Files.list(dir.toPath());
            Map<String, String> fileMap = new HashMap<>();
            list.forEach(path -> {
                File file = path.toFile();
                if (file.isFile()) {
                    String key = resolveFileLink(file);
                    fileMap.put(key, file.getName());
                } else {
                    Map<String, String> map;
                    try {
                        map = getAllFile(file);
                        fileMap.putAll(map);
                    } catch (IOException ex) {
                        log.warn("Cannot foreach file from dir '" + file + "'", ex);
                    }
                }
            });
            return fileMap;
        }
        return Collections.emptyMap();
    }

    private String resolveFileLink(File file) {
        String baseUri = baseDirectory.toURI().toString();
        String fileUri = file.toURI().toString();
        return basePath + fileUri.replace(baseUri, "");
    }

    private String resolveFileExt(String filename) {
        int index = filename.indexOf('.');
        if (index != -1) {
            return filename.substring(index);
        }
        return "";
    }

    public boolean match(String fileName) {
        if (fileName.matches(pattern)) {
            if (log.isTraceEnabled()) {
                log.debug("'{}' match with '{}'", fileName, pattern);
            }
            return true;
        }
        if (log.isTraceEnabled()) {
            log.debug("'{}' not match with {}", fileName, pattern);
        }
        return false;
    }

}
