package com.pkgfit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.AddService;
import com.pkgfit.service.CompatibilityService;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.PackageName;

@ShellComponent
public class InstallCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final AddService addService;
    private final RegistryService registryService;
    private final CompatibilityService compatibilityService;

    public InstallCommands(ContextService contextService, ResolverService resolverService,
            AddService addService, RegistryService registryService,
            CompatibilityService compatibilityService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.addService = addService;
        this.registryService = registryService;
        this.compatibilityService = compatibilityService;
    }

    @ShellMethod(value="Install dependencies: update all to latest matching versions.", key={"install", "i"})
    public String install(
            @ShellOption(defaultValue="", help="Package name(s)") String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Add as devDependency", value = "--dev") boolean dev){
        if (!packageName.isBlank()) {
            String[] parts = packageName.split("[ ,]");
            if (parts.length > 1) {
                return installMultiple(parts, dev);
            }
            return resolveSingle(packageName, dev);
        }
        return batchUpdate(dev);
    }

    private String installMultiple(String[] packages, boolean dev) {
        StringBuilder sb = new StringBuilder();
        int installed = 0;
        int failed = 0;
        for (String pkg : packages) {
            PackageName parsed = PackageName.parse(pkg);
            if (parsed.name().isBlank()) {
                sb.append(String.format("  \u2717 '%s' \u2014 invalid package name\n", pkg));
                failed++;
                continue;
            }
            ProjectContext context = contextService.detect();
            ResolutionResult result = resolverService.resolve(parsed.name(), parsed.range(), context);
            if (!result.hasResolution()) {
                sb.append(String.format("  \u2717 %s \u2014 could not resolve\n", pkg));
                failed++;
                continue;
            }

            String versionToUse = resolveWithCompatibility(parsed.name(), result.resolvedVersion(), context, sb);
            if (versionToUse == null) {
                sb.append(String.format("  \u2717 %s \u2014 no version compatible with existing deps\n", pkg));
                failed++;
                continue;
            }

            try {
                String rangeToWrite = parsed.range().isEmpty() ? "^" + versionToUse : parsed.range();
                addService.addDependency(parsed.name(), rangeToWrite, dev, Path.of("."));
                sb.append(String.format("  \u2713 %s@%s\n", parsed.name(), versionToUse));
                installed++;
            } catch (IOException e) {
                sb.append(String.format("  \u2717 %s \u2014 failed to write: %s\n", pkg, e.getMessage()));
                failed++;
            }
        }
        sb.append(String.format("\n%d installed, %d failed", installed, failed));
        return sb.toString();
    }

    private String resolveSingle(String input, boolean dev) {
        PackageName parsed = PackageName.parse(input);
        ProjectContext context = contextService.detect();
        ResolutionResult result = resolverService.resolve(parsed.name(), parsed.range(), context);
        if (!result.hasResolution()) {
            return "Could not resolve '" + input + "'.";
        }

        String versionToUse = resolveWithCompatibility(parsed.name(), result.resolvedVersion(), context, null);
        if (versionToUse == null) {
            return "Could not find a version of '" + parsed.name()
                    + "' compatible with existing dependencies.";
        }

        try {
            addService.addDependency(parsed.name(), parsed.range().isEmpty()
                    ? "^" + versionToUse : parsed.range(), dev, Path.of("."));
            String note = versionToUse.equals(result.resolvedVersion()) ? "" : " (auto-selected for compatibility)";
            return String.format("Installed %s@%s%s", parsed.name(), versionToUse, note);
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }

    private String resolveWithCompatibility(String name, String preferredVersion,
            ProjectContext context, StringBuilder sb) {
        JsonNode metadata = registryService.fetchPackageMetadata(name);
        if (metadata == null) {
            if (sb != null) sb.append(String.format("  \u2717 %s \u2014 not found in registry\n", name));
            return null;
        }
        String compatible = compatibilityService.findCompatibleVersion(metadata, preferredVersion, context);
        if (compatible == null) return null;
        if (!compatible.equals(preferredVersion) && sb != null) {
            sb.append(String.format("    (auto-selected %s for peer dep compatibility)\n", compatible));
        }
        return compatible;
    }

    private String batchUpdate(boolean dev) {
        ProjectContext context = contextService.detect();
        if (!context.packageJsonExists()) {
            return "No package.json found in current directory.";
        }

        Map<String, String> deps = context.existingDeps();
        if (deps.isEmpty()) {
            return "No dependencies found in package.json.";
        }

        StringBuilder sb = new StringBuilder();
        int updated = 0;
        int failed = 0;
        int unchanged = 0;

        for (Map.Entry<String, String> entry : deps.entrySet()) {
            String name = entry.getKey();
            String range = entry.getValue();

            ResolutionResult result = resolverService.resolve(name, range, context);
            if (!result.hasResolution()) {
                sb.append(String.format("  \u2717 %s@%s \u2014 could not resolve\n", name, range));
                failed++;
                continue;
            }

            String newRange = "^" + result.resolvedVersion();
            if (newRange.equals(range)) {
                sb.append(String.format("  \u2013 %s@%s \u2014 unchanged\n", name, range));
                unchanged++;
                continue;
            }

            try {
                addService.addDependency(name, newRange, dev, Path.of("."));
                sb.append(String.format("  \u2713 %s \u2014 %s \u2192 %s\n", name, range, newRange));
                updated++;
            } catch (IOException e) {
                sb.append(String.format("  \u2717 %s \u2014 failed to write: %s\n", name, e.getMessage()));
                failed++;
            }
        }

        sb.append(String.format("\n%d updated, %d unchanged, %d failed", updated, unchanged, failed));
        return sb.toString();
    }
}
