package com.pkgfit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.zafarkhaja.semver.Version;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.model.ResolutionResult.SkippedVersion;

@Service
public class ResolverService {

    private final RegistryService registryService;
     
    public ResolverService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public ResolutionResult resolve(String packageName, String versionRange, ProjectContext context) {
        JsonNode metadata = registryService.fetchPackageMetadata(packageName);
        if (metadata == null) {
            return new ResolutionResult(packageName, null, List.of(), false);
        }

        JsonNode versionsNode = metadata.get("versions");
        if (versionsNode == null || !versionsNode.isObject()) {
            return new ResolutionResult(packageName, null, List.of(), false);
        }

        boolean hasRange = versionRange != null && !versionRange.isBlank();
        boolean includePreRelease = hasRange && versionRange.contains("-");
        List<Version> matchingVersions = new ArrayList<>();
        List<SkippedVersion> skippedList = new ArrayList<>();

        versionsNode.fieldNames().forEachRemaining(versionStr -> {
            try {
                Version version = Version.parse(versionStr);
                boolean isPreRelease = !version.isStable();

                if (isPreRelease && !includePreRelease) {
                    skippedList.add(new SkippedVersion(versionStr,
                            "pre-release version not matched unless range specifies pre-release"));
                    return;
                }

                if (hasRange && !isPreRelease && !version.satisfies(versionRange)) {
                    skippedList.add(new SkippedVersion(versionStr,
                            "does not satisfy range '" + versionRange + "'"));
                    return;
                }

                matchingVersions.add(version);
            } catch (Exception e) {
                skippedList.add(new SkippedVersion(versionStr,
                        "invalid version format: " + e.getMessage()));
            }
        });

        Version bestVersion = matchingVersions.stream()
                .max(Version::compareTo)
                .orElse(null);

        String resolvedVersion = bestVersion != null ? bestVersion.toString() : null;

        boolean isAlreadyInstalled = false;
        Map<String, String> existingDeps = context.existingDeps();

        if (existingDeps.containsKey(packageName)) {
            try {
                Version installedVersion = Version.parse(existingDeps.get(packageName));
                boolean preReleaseOk = !hasRange || installedVersion.isStable() || includePreRelease;
                isAlreadyInstalled = preReleaseOk && (!hasRange || installedVersion.satisfies(versionRange));
            } catch (Exception ignored) {
            }
        }

        return new ResolutionResult(packageName, resolvedVersion, skippedList, isAlreadyInstalled);
    }
}
