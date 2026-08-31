package dev.lunaa.aod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class TestProjectFiles {
    private TestProjectFiles() {}

    static String read(String projectRelativePath) throws Exception {
        Path file = Paths.get(projectRelativePath);
        if (!Files.exists(file)) {
            file = Paths.get("..", projectRelativePath);
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
