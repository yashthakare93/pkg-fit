package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AddServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AddService addService = new AddService(mapper);

    @Test
    void addDependencyShouldCreatePackageJsonIfNotExists(@TempDir Path tempDir) throws Exception {
        addService.addDependency("lodash", "^4.17.21", false, tempDir);

        File pkgJson = tempDir.resolve("package.json").toFile();
        assertTrue(pkgJson.exists());

        ObjectNode root = (ObjectNode) mapper.readTree(pkgJson);
        assertEquals("^4.17.21", root.get("dependencies").get("lodash").asText());
    }

    @Test
    void addDependencyShouldAddToExistingDependencies(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"dependencies": {"express": "^4.18.0"}}
                """);

        addService.addDependency("lodash", "^1.0.0", false, tempDir);

        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals("^4.18.0", root.get("dependencies").get("express").asText());
        assertEquals("^1.0.0", root.get("dependencies").get("lodash").asText());
    }

    @Test
    void addDependencyShouldAddToDevDependencies(@TempDir Path tempDir) throws Exception {
        addService.addDependency("mocha", "^10.0.0", true, tempDir);

        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals("^10.0.0", root.get("devDependencies").get("mocha").asText());
        assertFalse(root.has("dependencies"));
    }

    @Test
    void addDependencyShouldPreserveExistingDevDeps(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"devDependencies": {"eslint": "^8.0.0"}}
                """);

        addService.addDependency("prettier", "^3.0.0", true, tempDir);

        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals("^8.0.0", root.get("devDependencies").get("eslint").asText());
        assertEquals("^3.0.0", root.get("devDependencies").get("prettier").asText());
    }

    @Test
    void addDependencyShouldPreserveExistingFields(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"name": "my-app", "version": "1.0.0"}
                """);

        addService.addDependency("react", "^18.0.0", false, tempDir);

        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals("my-app", root.get("name").asText());
        assertEquals("1.0.0", root.get("version").asText());
        assertEquals("^18.0.0", root.get("dependencies").get("react").asText());
    }
}
