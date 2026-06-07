package com.pkgfit.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ShellComponent
public class InitCommands {

    private final ObjectMapper objectMapper;
    private final Path workingDir;

    @Autowired
    public InitCommands(ObjectMapper objectMapper) {
        this(objectMapper, Path.of("."));
    }

    InitCommands(ObjectMapper objectMapper, Path workingDir) {
        this.objectMapper = objectMapper;
        this.workingDir = workingDir;
    }

    @ShellMethod(value = "Create a package.json if none exists.", key = {"init"})
    public String init(@ShellOption(defaultValue = "", help = "Project name") String name) {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (pkgJson.exists()) {
            return "package.json already exists.";
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            String projectName = name.isBlank()
                    ? workingDir.toAbsolutePath().getFileName().toString()
                    : name;
            root.put("name", projectName);
            root.put("version", "1.0.0");
            root.putObject("dependencies");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pkgJson, root);
            return "Created package.json.";
        } catch (IOException e) {
            return "Failed to create package.json: " + e.getMessage();
        }
    }
}
