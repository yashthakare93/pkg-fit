package com.pkgfit.commands;

import java.util.Iterator;
import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.model.ResolutionResult.SkippedVersion;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.PackageName;

@ShellComponent
public class WhyCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final RegistryService registryService;

    public WhyCommands(ContextService contextService, ResolverService resolverService,
            RegistryService registryService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.registryService = registryService;
    }

    @ShellMethod(value = "Show why a package resolved to a particular version.", key = {"why"})
    public String why(String packageName) {
        PackageName parsed = PackageName.parse(packageName);
        if (parsed.name().isBlank()) {
            return "Package name is required.";
        }

        ProjectContext context = contextService.detect();
        Map<String, String> deps = context.existingDeps();
        String installedRange = deps.get(parsed.name());

        boolean hasExplicitRange = !parsed.range().isEmpty();
        String rangeToUse = hasExplicitRange ? parsed.range() : installedRange;

        if (rangeToUse == null) {
            return String.format("Package '%s' is not in your dependencies and no range specified.",
                    parsed.name());
        }

        JsonNode metadata = registryService.fetchPackageMetadata(parsed.name());
        if (metadata == null) {
            return "Package '" + parsed.name() + "' not found in registry.";
        }

        ResolutionResult result = resolverService.resolve(parsed.name(), rangeToUse, context);
        String resolved = result.hasResolution() ? result.resolvedVersion() : "could not resolve";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%n", parsed.name()));
        sb.append(String.format("  Range:           %s%n", rangeToUse));
        sb.append(String.format("  Resolved:        %s%n", resolved));
        if (hasExplicitRange && installedRange != null) {
            sb.append(String.format("  Installed:       %s%n", installedRange));
        }
        sb.append(String.format("  Versions considered: %d%n",
                countVersions(metadata) + result.skippedVersions().size()));

        if (!result.skippedVersions().isEmpty()) {
            sb.append("\n  Skipped versions:\n");
            for (SkippedVersion sv : result.skippedVersions()) {
                sb.append(String.format("    \u2717 %s \u2014 %s%n", sv.version(), sv.reason()));
            }
        }

        if (result.hasResolution()) {
            JsonNode versions = metadata.get("versions");
            if (versions != null && versions.has(resolved)) {
                JsonNode peerDeps = versions.get(resolved).get("peerDependencies");
                if (peerDeps != null && peerDeps.isObject() && !peerDeps.isEmpty()) {
                    sb.append("\n  Peer dependencies:\n");
                    Iterator<String> fields = peerDeps.fieldNames();
                    while (fields.hasNext()) {
                        String dep = fields.next();
                        String range = peerDeps.get(dep).asText();
                        String status = deps.containsKey(dep)
                                ? String.format(" (installed: %s)", deps.get(dep))
                                : " (not installed)";
                        sb.append(String.format("    %s@%s%s%n", dep, range, status));
                    }
                } else {
                    sb.append("\n  Peer dependencies: none\n");
                }
            }
        }

        return sb.toString();
    }

    private int countVersions(JsonNode metadata) {
        JsonNode versions = metadata.get("versions");
        if (versions == null || !versions.isObject()) return 0;
        int count = 0;
        Iterator<String> it = versions.fieldNames();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }
}
