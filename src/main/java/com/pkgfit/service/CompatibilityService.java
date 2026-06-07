package com.pkgfit.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.zafarkhaja.semver.Version;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;

@Service
public class CompatibilityService {

    private final ResolverService resolverService;

    public CompatibilityService(ResolverService resolverService) {
        this.resolverService = resolverService;
    }

    public String findCompatibleVersion(JsonNode metadata, String preferredVersion, ProjectContext context) {
        JsonNode versions = metadata.get("versions");
        if (versions == null || !versions.isObject()) return null;

        List<Version> candidates = new ArrayList<>();
        versions.fieldNames().forEachRemaining(v -> {
            try {
                candidates.add(Version.parse(v));
            } catch (Exception ignored) {}
        });
        candidates.sort(Comparator.reverseOrder());

        Version preferred;
        try {
            preferred = Version.parse(preferredVersion);
        } catch (Exception e) {
            return null;
        }

        int startIndex = candidates.indexOf(preferred);
        boolean foundPreferred = startIndex >= 0;
        if (!foundPreferred) startIndex = 0;

        for (int i = startIndex; i < candidates.size(); i++) {
            Version candidate = candidates.get(i);
            JsonNode versionMeta = versions.get(candidate.toString());
            if (isCompatible(versionMeta, context)) {
                return candidate.toString();
            }
        }

        if (foundPreferred) {
            for (int i = startIndex - 1; i >= 0; i--) {
                Version candidate = candidates.get(i);
                JsonNode versionMeta = versions.get(candidate.toString());
                if (isCompatible(versionMeta, context)) {
                    return candidate.toString();
                }
            }
        }

        return null;
    }

    private boolean isCompatible(JsonNode versionMeta, ProjectContext context) {
        JsonNode peerDeps = versionMeta.get("peerDependencies");
        if (peerDeps == null || !peerDeps.isObject() || peerDeps.isEmpty()) return true;

        Map<String, String> existing = context.existingDeps();
        if (existing.isEmpty()) return true;

        Iterator<String> fields = peerDeps.fieldNames();
        while (fields.hasNext()) {
            String depName = fields.next();
            String requiredRange = peerDeps.get(depName).asText();

            String installedRange = existing.get(depName);
            if (installedRange == null) continue;

            ResolutionResult depResult = resolverService.resolve(depName, installedRange, context);
            if (!depResult.hasResolution()) continue;

            try {
                Version installedVersion = Version.parse(depResult.resolvedVersion());
                if (!installedVersion.satisfies(requiredRange)) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }
        return true;
    }
}
