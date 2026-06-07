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
import com.pkgfit.util.Colors;
import com.pkgfit.util.PackageName;
import com.pkgfit.util.Spinner;

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

        Spinner.start("Investigating " + parsed.name());
        try {
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
                return Colors.red("Package '" + parsed.name() + "' not found in registry.");
            }

            ResolutionResult result = resolverService.resolve(parsed.name(), rangeToUse, context);
            String resolved = result.hasResolution() ? result.resolvedVersion() : Colors.yellow("could not resolve");

            int totalVersions = countVersions(metadata);
            int skippedCount = result.skippedVersions().size();
            int inRange = totalVersions - skippedCount;

            StringBuilder sb = new StringBuilder();
            sb.append(Colors.bold(parsed.name())).append("\n");
            sb.append(String.format("  %s       %s%n", Colors.dim("Range:"), Colors.yellow(rangeToUse)));
            sb.append(String.format("  %s    %s%n", Colors.dim("Resolved:"), Colors.green(resolved)));
            if (hasExplicitRange && installedRange != null) {
                sb.append(String.format("  %s     %s%n", Colors.dim("Installed:"), Colors.yellow(installedRange)));
            }
            sb.append(String.format("  %s %d  %s%n",
                    Colors.dim("Versions:"),
                    totalVersions,
                    Colors.dim("(" + inRange + " in range, " + skippedCount + " outside range)")));

            long interestingSkips = result.skippedVersions().stream()
                    .filter(sv -> !sv.reason().toLowerCase().contains("does not satisfy range"))
                    .count();

            if (interestingSkips > 0) {
                sb.append("\n  ").append(Colors.bold("Interesting:")).append("\n");
                int limit = 20;
                int shown = 0;
                for (SkippedVersion sv : result.skippedVersions()) {
                    if (sv.reason().toLowerCase().contains("does not satisfy range")) continue;
                    if (shown >= limit) {
                        sb.append(String.format("    %s ... and %d more%n",
                                Colors.dim("\u22EF"), interestingSkips - limit));
                        break;
                    }
                    sb.append(String.format("    %s %s  %s%n", Colors.dim("\u2717"), Colors.dim(sv.version()), Colors.dim(sv.reason())));
                    shown++;
                }
            }

            if (result.hasResolution()) {
                JsonNode versions = metadata.get("versions");
                if (versions != null && versions.has(resolved)) {
                    JsonNode peerDeps = versions.get(resolved).get("peerDependencies");
                    if (peerDeps != null && peerDeps.isObject() && !peerDeps.isEmpty()) {
                        sb.append("\n  ").append(Colors.bold("Peer dependencies:")).append("\n");
                        Iterator<String> fields = peerDeps.fieldNames();
                        while (fields.hasNext()) {
                            String dep = fields.next();
                            String range = peerDeps.get(dep).asText();
                            String status = deps.containsKey(dep)
                                    ? Colors.green("  (installed: " + deps.get(dep) + ")")
                                    : Colors.dim("  (not installed)");
                            sb.append(String.format("    %s  %s%s%n", Colors.cyan(dep), Colors.yellow(range), status));
                        }
                    }
                }
            }

            return sb.toString();
        } finally {
            Spinner.stop();
        }
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
