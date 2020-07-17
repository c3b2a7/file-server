package me.lolico.fileserver.web;

import me.lolico.fileserver.util.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lolico
 */
public class UploadController implements RestControllerMark {

    private final FileUtils fileUtils;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public UploadController(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    @PostMapping("/uploads")
    public ResponseEntity<Object> upload(@RequestParam("file") MultipartFile file) {
        String link;
        try {
            link = fileUtils.upload(file);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
        if (link != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("_link", link);
            map.put("_self", link.substring(link.lastIndexOf('/') + 1));
            return ResponseEntity.ok(map);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/uploads")
    public Map<String, String> allFile() throws IOException {
        return fileUtils.getAllFile();
    }

    @GetMapping("/uploads/{y}/{m}/{d}")
    public Map<String, String> allFile(@PathVariable String y,
                                       @PathVariable String m,
                                       @PathVariable String d) throws IOException {
        String basePath = fileUtils.getBaseDirectory().getPath();
        Path path = Paths.get(basePath, y, m, d);
        return fileUtils.getAllFile(path.toFile());
    }

    @GetMapping("/uploads/{name}")
    public void get(@PathVariable String name, HttpServletResponse response) throws IOException {
        File file = fileUtils.getFile(name);
        response(response, file);
    }

    @GetMapping("/uploads/{y}/{m}/{d}/{name}")
    public void get(@PathVariable Integer y,
                    @PathVariable Integer m,
                    @PathVariable Integer d,
                    @PathVariable String name,
                    HttpServletResponse response) throws IOException {
        File file = fileUtils.getFile(name, LocalDate.of(y, m, d));
        response(response, file);
    }

    @DeleteMapping("/uploads/{name}")
    public ResponseEntity<Object> delete(@PathVariable String name) {
        boolean delete = fileUtils.deleteFile(name);
        if (delete) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/uploads/{y}/{m}/{d}/{name}")
    public ResponseEntity<Object> delete(@PathVariable Integer y,
                                         @PathVariable Integer m,
                                         @PathVariable Integer d,
                                         @PathVariable String name) {
        boolean delete = fileUtils.deleteFile(name, LocalDate.of(y, m, d));
        if (delete) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }

    private void response(HttpServletResponse response, File file) throws IOException {
        if (file.exists() && file.isFile()) {
            String contentType = Files.probeContentType(file.toPath());
            String mediaType = contentType == null ? DEFAULT_CONTENT_TYPE : contentType;
            response.setContentType(MediaType.parseMediaType(mediaType).toString());
            try (FileInputStream in = new FileInputStream(file)) {
                StreamUtils.copy(in, response.getOutputStream());
            }
            return;
        }
        response.setStatus(HttpStatus.NOT_FOUND.value());
    }
}
