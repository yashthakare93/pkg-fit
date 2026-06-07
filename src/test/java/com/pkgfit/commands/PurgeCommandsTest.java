package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pkgfit.util.Colors;

class PurgeCommandsTest {

    private ObjectMapper mapper;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.mapper = new ObjectMapper();
        this.tempDir = tempDir;
    }

    @Test
    void noPackageJson() {
        PurgeCommands commands = new PurgeCommands(mapper, tempDir);
        String output = commands.purge(false);
        assertTrue(Colors.strip(output).contains("No package.json"));
    }

    @Test
    void purgeAll() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("dependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        root.set("devDependencies", mapper.createObjectNode().put("mocha", "^10.0.0"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        PurgeCommands commands = new PurgeCommands(mapper, tempDir);
        String output = commands.purge(false);

        assertTrue(Colors.strip(output).contains("Purged dependencies"));

        ObjectNode after = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals(false, after.has("dependencies"));
        assertEquals(false, after.has("devDependencies"));
    }

    @Test
    void purgeDevOnly() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("dependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        root.set("devDependencies", mapper.createObjectNode().put("mocha", "^10.0.0"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        PurgeCommands commands = new PurgeCommands(mapper, tempDir);
        String output = commands.purge(true);

        assertTrue(Colors.strip(output).contains("Purged devDependencies"));

        ObjectNode after = (ObjectNode) mapper.readTree(tempDir.resolve("package.json").toFile());
        assertEquals(true, after.has("dependencies"));
        assertEquals(false, after.has("devDependencies"));
    }

    @Test
    void purgeNoDeps() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("name", "test");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        PurgeCommands commands = new PurgeCommands(mapper, tempDir);
        String output = commands.purge(false);

        assertTrue(Colors.strip(output).contains("No dependencies"));
    }

    @Test
    void purgeNoDevDeps() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("dependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        PurgeCommands commands = new PurgeCommands(mapper, tempDir);
        String output = commands.purge(true);

        assertTrue(Colors.strip(output).contains("No devDependencies"));
    }
}
