package com.pkgfit.model;

import java.util.List;

public record ResolutionResult(
    String packageName,
    String resolvedVersion,
    List<SkippedVersion> skippedVersions,
    boolean isAlreadyInstalled
    
) {
    public boolean hasResolution(){
        return resolvedVersion != null && !resolvedVersion.isEmpty();
    }

    public record SkippedVersion(String version, String reason) {}
}

