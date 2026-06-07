package com.pkgfit.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkgfit.model.ProjectContext;

@Service
public class ContextService {
    private static final Logger log = LoggerFactory.getLogger(ContextService.class);
    private final ObjectMapper objectMapper;
    private final Path workingDir;

    @Autowired
    public ContextService(ObjectMapper objectMapper) {
        this(objectMapper, Path.of("."));
    }

    ContextService(ObjectMapper objectMapper, Path workingDir) {
        this.objectMapper = objectMapper;
        this.workingDir = workingDir;
    }

    public ProjectContext detect() {
        String nodeVersion = detectNodeVersion();
        Map<String, String> deps = readPackageJsonDeps();
        String os = System.getProperty("os.name", "unknown");
        String arch = System.getProperty("os.arch", "unknown");
        boolean pkgJsonExists = workingDir.resolve("package.json").toFile().exists();
        return new ProjectContext(nodeVersion, deps, os, arch, pkgJsonExists);
    }

    String detectNodeVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder("node", "--version");
            pb.redirectErrorStream(true);
            pb.directory(workingDir.toFile());
            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && output != null) {
                String version = output.trim();
                if (version.startsWith("v") || version.startsWith("V")) {
                    return version.substring(1);
                }
                return version;
            }

        } catch (Exception e) {
            log.warn("Error detecting Node.js version: {}", e.getMessage());
        }
        return "0.0.0";
    }

    public Map<String, String> readDepsOnly() {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (!pkgJson.exists()) return Map.of();
        Map<String, String> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(pkgJson);
            extractDepsInto(root.path("dependencies"), result);
        } catch (Exception e) {
            log.warn("Failed to parse package.json: {}", e.getMessage());
        }
        return Map.copyOf(result);
    }

    public Map<String, String> readDevDepsOnly() {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (!pkgJson.exists()) return Map.of();
        Map<String, String> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(pkgJson);
            extractDepsInto(root.path("devDependencies"), result);
        } catch (Exception e) {
            log.warn("Failed to parse package.json: {}", e.getMessage());
        }
        return Map.copyOf(result);
    }

    Map<String, String> readPackageJsonDeps() {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (!pkgJson.exists()) return Map.of();
        Map<String, String> combined = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(pkgJson);
            extractDepsInto(root.path("dependencies"), combined);
            extractDepsInto(root.path("devDependencies"), combined);
        } catch (Exception e) {
            log.warn("Failed to parse package.json: {}", e.getMessage());
        }
        return Map.copyOf(combined);
    }

    private void extractDepsInto(JsonNode depsNode, Map<String, String> target) {
        if (depsNode.isMissingNode() || !depsNode.isObject()) return;
        depsNode.fields().forEachRemaining(e -> target.put(e.getKey(), e.getValue().asText()));
    }
}
