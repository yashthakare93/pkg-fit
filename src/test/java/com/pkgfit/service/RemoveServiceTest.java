package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoveServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RemoveService removeService = new RemoveService(mapper);

    @Test
    void removeExistingDep(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"dependencies": {"lodash": "^4.0.0"}}
                """);

        boolean result = removeService.removeDependency("lodash", false, tempDir);

        assertTrue(result);
        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertFalse(root.has("dependencies"));
    }

    @Test
    void removeNonExistentDep(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"dependencies": {"express": "^4.0.0"}}
                """);

        boolean result = removeService.removeDependency("lodash", false, tempDir);

        assertFalse(result);
        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertTrue(root.has("dependencies"));
        assertTrue(root.get("dependencies").has("express"));
    }

    @Test
    void removeFromDevDependencies(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"devDependencies": {"mocha": "^10.0.0"}}
                """);

        boolean result = removeService.removeDependency("mocha", true, tempDir);

        assertTrue(result);
        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertFalse(root.has("devDependencies"));
    }

    @Test
    void removeWhenNoPackageJson(@TempDir Path tempDir) throws Exception {
        boolean result = removeService.removeDependency("lodash", false, tempDir);
        assertFalse(result);
    }

    @Test
    void removeLastDepCleansUpKey(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"dependencies": {"react": "^18.0.0"}}
                """);

        boolean result = removeService.removeDependency("react", false, tempDir);

        assertTrue(result);
        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertFalse(root.has("dependencies"));
    }

    @Test
    void removeDepPreservesOtherFields(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"name": "app", "dependencies": {"a": "1.0", "b": "2.0"}}
                """);

        boolean result = removeService.removeDependency("a", false, tempDir);

        assertTrue(result);
        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals("app", root.get("name").asText());
        assertFalse(root.get("dependencies").has("a"));
        assertTrue(root.get("dependencies").has("b"));
    }
}
