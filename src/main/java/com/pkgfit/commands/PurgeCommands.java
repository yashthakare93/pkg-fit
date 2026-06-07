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
import com.pkgfit.util.Colors;

@ShellComponent
public class PurgeCommands {

    private final ObjectMapper objectMapper;
    private final Path workingDir;

    @Autowired
    public PurgeCommands(ObjectMapper objectMapper) {
        this(objectMapper, Path.of("."));
    }

    PurgeCommands(ObjectMapper objectMapper, Path workingDir) {
        this.objectMapper = objectMapper;
        this.workingDir = workingDir;
    }

    @ShellMethod(value = "Remove all dependencies from package.json.", key = {"purge", "prune"})
    public String purge(
            @ShellOption(arity = 0, defaultValue = "false", help = "Remove only devDependencies", value = "--dev") boolean dev) {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (!pkgJson.exists()) {
            return Colors.red("No package.json found in current directory.");
        }
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(pkgJson);
            if (dev) {
                if (!root.has("devDependencies")) {
                    return Colors.yellow("No devDependencies found.");
                }
                root.remove("devDependencies");
            } else {
                boolean hadDeps = root.has("dependencies");
                boolean hadDevDeps = root.has("devDependencies");
                if (!hadDeps && !hadDevDeps) {
                    return Colors.yellow("No dependencies found.");
                }
                root.remove("dependencies");
                root.remove("devDependencies");
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pkgJson, root);
            return Colors.green("Purged ") + (dev ? Colors.cyan("devDependencies") : Colors.cyan("dependencies")) + ".";
        } catch (IOException e) {
            return Colors.red("Failed to write package.json: " + e.getMessage());
        }
    }
}
