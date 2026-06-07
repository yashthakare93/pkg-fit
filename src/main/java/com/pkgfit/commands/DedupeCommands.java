package com.pkgfit.commands;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkgfit.util.Colors;

@ShellComponent
public class DedupeCommands {

    private final ObjectMapper objectMapper;
    private final Path workingDir;

    @Autowired
    public DedupeCommands(ObjectMapper objectMapper) {
        this(objectMapper, Path.of("."));
    }

    DedupeCommands(ObjectMapper objectMapper, Path workingDir) {
        this.objectMapper = objectMapper;
        this.workingDir = workingDir;
    }

    @ShellMethod(value = "Find duplicate dependencies across deps and devDeps.", key = {"dedupe", "dd"})
    public String dedupe() {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (!pkgJson.exists()) {
            return Colors.red("No package.json found in current directory.");
        }
        try {
            JsonNode root = objectMapper.readTree(pkgJson);

            Map<String, String> deps = readDeps(root.get("dependencies"));
            Map<String, String> devDeps = readDeps(root.get("devDependencies"));

            if (deps.isEmpty() && devDeps.isEmpty()) {
                return Colors.yellow("No dependencies found in package.json.");
            }

            Set<String> allNames = new HashSet<>(deps.keySet());
            allNames.addAll(devDeps.keySet());

            StringBuilder sb = new StringBuilder();
            int dupCount = 0;

            for (String name : allNames) {
                boolean inDeps = deps.containsKey(name);
                boolean inDevDeps = devDeps.containsKey(name);
                String depsRange = deps.get(name);
                String devDepsRange = devDeps.get(name);

                if (inDeps && inDevDeps && !depsRange.equals(devDepsRange)) {
                    dupCount++;
                    sb.append("  ").append(Colors.bold(Colors.yellow(name))).append("\n");
                    sb.append("    ").append(Colors.dim("dependencies:      ")).append(Colors.cyan(depsRange)).append("\n");
                    sb.append("    ").append(Colors.dim("devDependencies:   ")).append(Colors.magenta(devDepsRange)).append("\n");
                }
            }

            if (dupCount == 0) {
                return Colors.green("No duplicate dependencies found with conflicting ranges.");
            }

            sb.insert(0, Colors.bold("Found " + dupCount + " duplicate(s):") + "\n");
            return sb.toString();

        } catch (Exception e) {
            return Colors.red("Failed to read package.json: " + e.getMessage());
        }
    }

    private Map<String, String> readDeps(JsonNode depsNode) {
        Map<String, String> result = new HashMap<>();
        if (depsNode == null || !depsNode.isObject()) return result;
        Iterator<String> fields = depsNode.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            result.put(name, depsNode.get(name).asText());
        }
        return result;
    }
}
