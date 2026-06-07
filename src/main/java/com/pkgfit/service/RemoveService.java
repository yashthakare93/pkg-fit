package com.pkgfit.service;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Path;


@Service
public class RemoveService {
    
    private final ObjectMapper objectMapper;

    @Autowired
    public RemoveService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean removeDependency(String name, boolean dev, Path workingDir) throws IOException {
        File pkgJson = workingDir.resolve("package.json").toFile();
        if (!pkgJson.exists()) {
            return false;
        }
        ObjectNode root = (ObjectNode) objectMapper.readTree(pkgJson);
        String depsKey = dev ? "devDependencies" : "dependencies";
        if (!root.has(depsKey)) {
            return false;
        }
        ObjectNode deps = (ObjectNode) root.get(depsKey);
        if (!deps.has(name)) {
            return false;
        }
        deps.remove(name);
        if (deps.isEmpty()) {
            root.remove(depsKey);
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(pkgJson, root);
        return true;
    }
}
