package me.lolico.fileserver.util;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

class FileUtilsTest {

    @Test
    void name() throws IOException {
        File baseDir = new File("uploads");
        LocalDate localDate = LocalDate.now();
        String format = localDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        System.out.println(format);
        File dir = new File(baseDir, format);
        File file = new File(dir, "tmp.txt");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath())) {
            bufferedWriter.write("测试2");
        }
    }

    @Test
    void name2() {
        String dirf = "uploads\\tmp.txt";
        String filename = "tmp.txt";
        if (filename.matches("^*.*")) {
            System.out.println(1);
        }
        String replace = dirf.replace(".", dirf);
        System.out.println(replace);
    }

}