package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkgfit.model.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContextServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void detectShouldReturnEmptyContextWhenNoPackageJson(@TempDir Path emptyDir) {
        ContextService service = new ContextService(new ObjectMapper(), emptyDir);
        ProjectContext context = service.detect();

        assertNotNull(context);
        assertFalse(context.packageJsonExists());
        assertTrue(context.existingDeps().isEmpty());
    }

    @Test
    void readPackageJsonDepsShouldParseDependencies() throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {
                  "dependencies": {
                    "express": "^4.18.0",
                    "lodash": "^1.0.0"
                  },
                  "devDependencies": {
                    "mocha": "^10.0.0"
                  }
                }
                """);

        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        Map<String, String> deps = service.readPackageJsonDeps();

        assertEquals(3, deps.size());
        assertEquals("^4.18.0", deps.get("express"));
        assertEquals("^1.0.0", deps.get("lodash"));
        assertEquals("^10.0.0", deps.get("mocha"));
    }

    @Test
    void readPackageJsonDepsShouldReturnEmptyMapWhenNoFile() {
        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        Map<String, String> deps = service.readPackageJsonDeps();

        assertTrue(deps.isEmpty());
    }

    @Test
    void readPackageJsonDepsShouldReturnEmptyMapWhenNoDepsKeys() throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"name": "test"}
                """);

        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        Map<String, String> deps = service.readPackageJsonDeps();

        assertTrue(deps.isEmpty());
    }

    @Test
    void readPackageJsonDepsShouldHandleMalformedJson() throws Exception {
        Files.writeString(tempDir.resolve("package.json"), "{invalid json}");

        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        Map<String, String> deps = service.readPackageJsonDeps();

        assertTrue(deps.isEmpty());
    }

    @Test
    void detectShouldReportPackageJsonExists() throws Exception {
        Files.writeString(tempDir.resolve("package.json"), """
                {"dependencies": {"react": "^18.0.0"}}
                """);

        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        ProjectContext context = service.detect();

        assertTrue(context.packageJsonExists());
        assertEquals(1, context.existingDeps().size());
        assertEquals("^18.0.0", context.existingDeps().get("react"));
    }

    @Test
    void detectShouldReportSystemOsAndArch() {
        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        ProjectContext context = service.detect();

        assertEquals(System.getProperty("os.name"), context.os());
        assertEquals(System.getProperty("os.arch"), context.arch());
    }

    @Test
    void detectNodeVersionShouldReturnVersionString() {
        ContextService service = new ContextService(new ObjectMapper(), tempDir);
        String version = service.detectNodeVersion();

        // Node may or may not be installed in the test env
        assertNotNull(version);
    }
}
