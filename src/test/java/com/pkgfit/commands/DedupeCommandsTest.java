package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class DedupeCommandsTest {

    private ObjectMapper mapper;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.mapper = new ObjectMapper();
        this.tempDir = tempDir;
    }

    @Test
    void noPackageJson() {
        DedupeCommands commands = new DedupeCommands(mapper, tempDir);
        String output = commands.dedupe();
        assertTrue(output.contains("No package.json"));
    }

    @Test
    void noDupes() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("dependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        root.set("devDependencies", mapper.createObjectNode().put("mocha", "^10.0.0"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        DedupeCommands commands = new DedupeCommands(mapper, tempDir);
        String output = commands.dedupe();
        assertTrue(output.contains("No duplicate"));
    }

    @Test
    void findsDuplicates() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("dependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        root.set("devDependencies", mapper.createObjectNode().put("lodash", "^3.0.0"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        DedupeCommands commands = new DedupeCommands(mapper, tempDir);
        String output = commands.dedupe();
        assertTrue(output.contains("lodash"));
        assertTrue(output.contains("^4.0.0"));
        assertTrue(output.contains("^3.0.0"));
    }

    @Test
    void sameRangeNotDuplicate() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("dependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        root.set("devDependencies", mapper.createObjectNode().put("lodash", "^4.0.0"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(tempDir.resolve("package.json").toFile(), root);

        DedupeCommands commands = new DedupeCommands(mapper, tempDir);
        String output = commands.dedupe();
        assertFalse(output.contains("lodash"));
        assertTrue(output.contains("No duplicate"));
    }
}
