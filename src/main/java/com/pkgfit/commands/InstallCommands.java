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
import com.pkgfit.service.NpmService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.PackageName;
import com.pkgfit.util.Spinner;

@ShellComponent
public class InstallCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final AddService addService;
    private final RegistryService registryService;
    private final CompatibilityService compatibilityService;
    private final NpmService npmService;

    public InstallCommands(ContextService contextService, ResolverService resolverService,
            AddService addService, RegistryService registryService,
            CompatibilityService compatibilityService, NpmService npmService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.addService = addService;
        this.registryService = registryService;
        this.compatibilityService = compatibilityService;
        this.npmService = npmService;
    }

    @ShellMethod(value="Install dependencies: update all to latest matching versions.", key={"install", "i"})
    public String install(
            @ShellOption(defaultValue="", help="Package name(s)") String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Add as devDependency", value = "--dev") boolean dev,
            @ShellOption(arity = 0, defaultValue = "false", help = "Run npm install after", value = "--install") boolean install){
        if (!packageName.isBlank()) {
            String[] parts = packageName.split("[ ,]");
            if (parts.length > 1) {
                return installMultiple(parts, dev, install);
            }
            return resolveSingle(packageName, dev, install);
        }
        return batchUpdate(dev);
    }

    private String installMultiple(String[] packages, boolean dev, boolean runNpm) {
        Spinner.start("Installing " + packages.length + " packages");
        try {
            StringBuilder sb = new StringBuilder();
            int installed = 0;
            int failed = 0;
            for (String pkg : packages) {
                PackageName parsed = PackageName.parse(pkg);
                if (parsed.name().isBlank()) {
                    sb.append("  ").append(Colors.red("\u2717")).append(" '").append(pkg).append("' \u2014 ").append(Colors.red("invalid package name")).append("\n");
                    failed++;
                    continue;
                }
                ProjectContext context = contextService.detect();
                ResolutionResult result = resolverService.resolve(parsed.name(), parsed.range(), context);
                if (!result.hasResolution()) {
                    sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(parsed.name())).append(" \u2014 ").append(Colors.red("could not resolve")).append("\n");
                    failed++;
                    continue;
                }

                String versionToUse = resolveWithCompatibility(parsed.name(), result.resolvedVersion(), context, sb);
                if (versionToUse == null) {
                    sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(parsed.name())).append(" \u2014 ").append(Colors.red("no version compatible with existing deps")).append("\n");
                    failed++;
                    continue;
                }

                try {
                    String rangeToWrite = parsed.range().isEmpty() ? "^" + versionToUse : parsed.range();
                    addService.addDependency(parsed.name(), rangeToWrite, dev, Path.of("."));
                    sb.append("  ").append(Colors.green("\u2713")).append(" ").append(Colors.cyan(parsed.name())).append("@").append(Colors.bold(versionToUse)).append("\n");
                    installed++;
                } catch (IOException e) {
                    sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(parsed.name())).append(" \u2014 ").append(Colors.red("failed to write: " + e.getMessage())).append("\n");
                    failed++;
                }
            }
            sb.append("\n").append(Colors.bold(installed + " installed")).append(", ").append(Colors.red(failed + " failed"));
            if (runNpm && installed > 0) {
                sb.append("\n").append(npmService.install(Path.of(".")));
            }
            return sb.toString();
        } finally {
            Spinner.stop();
        }
    }

    private String resolveSingle(String input, boolean dev, boolean runNpm) {
        PackageName parsed = PackageName.parse(input);
        Spinner.start("Resolving " + parsed.name());
        try {
            ProjectContext context = contextService.detect();
            ResolutionResult result = resolverService.resolve(parsed.name(), parsed.range(), context);
            if (!result.hasResolution()) {
                return Colors.red("Could not resolve '" + input + "'.");
            }

            String versionToUse = resolveWithCompatibility(parsed.name(), result.resolvedVersion(), context, null);
            if (versionToUse == null) {
                return Colors.red("Could not find a version of '" + parsed.name()
                        + "' compatible with existing dependencies.");
            }

            try {
                addService.addDependency(parsed.name(), parsed.range().isEmpty()
                        ? "^" + versionToUse : parsed.range(), dev, Path.of("."));
                String note = versionToUse.equals(result.resolvedVersion()) ? "" : Colors.dim(" (auto-selected for compatibility)");
                String out = Colors.green("Installed ") + Colors.cyan(parsed.name()) + "@" + Colors.bold(versionToUse) + note;
                if (runNpm) {
                    out += "\n" + npmService.install(Path.of("."));
                }
                return out;
            } catch (IOException e) {
                return Colors.red("Failed to write package.json: " + e.getMessage());
            }
        } finally {
            Spinner.stop();
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
            return Colors.red("No package.json found in current directory.");
        }

        Map<String, String> deps = context.existingDeps();
        if (deps.isEmpty()) {
            return Colors.yellow("No dependencies found in package.json.");
        }

        Spinner.start("Resolving " + deps.size() + " dependencies");
        try {
            StringBuilder sb = new StringBuilder();
            int updated = 0;
            int failed = 0;
            int unchanged = 0;

            for (Map.Entry<String, String> entry : deps.entrySet()) {
                String name = entry.getKey();
                String range = entry.getValue();

                ResolutionResult result = resolverService.resolve(name, range, context);
                if (!result.hasResolution()) {
                    sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(name)).append("@").append(Colors.yellow(range)).append(" \u2014 ").append(Colors.red("could not resolve")).append("\n");
                    failed++;
                    continue;
                }

                String newRange = "^" + result.resolvedVersion();
                if (newRange.equals(range)) {
                    sb.append("  \u2013 ").append(Colors.cyan(name)).append("@").append(Colors.yellow(range)).append(" \u2014 ").append(Colors.dim("unchanged")).append("\n");
                    unchanged++;
                    continue;
                }

                try {
                    addService.addDependency(name, newRange, dev, Path.of("."));
                    sb.append("  ").append(Colors.green("\u2713")).append(" ").append(Colors.cyan(name)).append(" \u2014 ").append(Colors.yellow(range)).append(" ").append(Colors.dim("\u2192")).append(" ").append(Colors.green(newRange)).append("\n");
                    updated++;
                } catch (IOException e) {
                    sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(name)).append(" \u2014 ").append(Colors.red("failed to write: " + e.getMessage())).append("\n");
                    failed++;
                }
            }

            sb.append("\n").append(Colors.bold(updated + " updated")).append(", ").append(Colors.dim(unchanged + " unchanged")).append(", ").append(Colors.red(failed + " failed"));
            return sb.toString();
        } finally {
            Spinner.stop();
        }
    }
}
