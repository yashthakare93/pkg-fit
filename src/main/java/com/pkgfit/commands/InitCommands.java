package com.pkgfit.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ShellComponent
public class InitCommands {

    private final ObjectMapper objectMapper;

    public InitCommands(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ShellMethod(value = "Create a package.json if none exists.", key = {"init"})
    public String init() {
        File pkgJson = Path.of(".").resolve("package.json").toFile();
        if (pkgJson.exists()) {
            return "package.json already exists.";
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("name", Path.of(".").toAbsolutePath().getFileName().toString());
            root.put("version", "1.0.0");
            root.putObject("dependencies");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pkgJson, root);
            return "Created package.json.";
        } catch (IOException e) {
            return "Failed to create package.json: " + e.getMessage();
        }
    }
}
