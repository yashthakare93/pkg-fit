package com.pkgfit.service;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

@Service
public class AddService {
    private final ObjectMapper objectMapper;

    public AddService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addDependency(String name, String range, boolean dev, Path workingDir) throws IOException {

        File pkgJson = workingDir.resolve("package.json").toFile();
        ObjectNode root;

        if (pkgJson.exists()) {
            root = (ObjectNode) objectMapper.readTree(pkgJson);
        } else {
            root = objectMapper.createObjectNode();
        }

        String depsKey = dev ? "devDependencies" : "dependencies";
        ObjectNode deps;
        if (root.has(depsKey)) {
            deps = (ObjectNode) root.get(depsKey);
        } else {
            deps = root.putObject(depsKey);
        }

        deps.put(name, range);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(pkgJson, root);

    }
}
