package com.pkgfit.model;

import java.util.Map;

public record ProjectContext(
    String nodeVersion,
    Map<String, String> existingDeps,
    String os,
    String arch,
    boolean packageJsonExists) {

    public static ProjectContext empty(){
        return new ProjectContext("0.0.0", Map.of(),
            System.getProperty("os.name"), System.getProperty("os.arch"), false);
    }
}