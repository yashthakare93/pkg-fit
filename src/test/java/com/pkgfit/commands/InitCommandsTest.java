package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InitCommandsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final InitCommands commands = new InitCommands(mapper);

    @Test
    void initCreatesPackageJson(@TempDir Path tempDir) {
        InitCommands cmd = new InitCommands(mapper, tempDir);
        String result = cmd.init("test-project");

        assertEquals("Created package.json.", result);
        assertTrue(tempDir.resolve("package.json").toFile().exists());
    }

    @Test
    void initReturnsAlreadyExists(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("package.json"), "{}");
        InitCommands cmd = new InitCommands(mapper, tempDir);

        String result = cmd.init("test");

        assertEquals("package.json already exists.", result);
    }

    @Test
    void initCreatesWithGivenName(@TempDir Path tempDir) throws Exception {
        InitCommands cmd = new InitCommands(mapper, tempDir);
        cmd.init("my-app");

        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals("my-app", root.get("name").asText());
        assertEquals("1.0.0", root.get("version").asText());
        assertTrue(root.has("dependencies"));
    }

    @Test
    void initUsesDirNameWhenNoNameGiven(@TempDir Path tempDir) throws Exception {
        InitCommands cmd = new InitCommands(mapper, tempDir);
        cmd.init("");

        ObjectNode root = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals(tempDir.getFileName().toString(), root.get("name").asText());
    }
}
