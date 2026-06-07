package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.Spinner;

@ShellComponent
public class DescribeCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final RegistryService registryService;

    public DescribeCommands(ContextService contextService, ResolverService resolverService, RegistryService registryService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.registryService = registryService;
    }

    @ShellMethod(value = "Show detailed info about a package.", key = {"describe", "desc"})
    public String describe(String packageName) {
        Spinner.start("Fetching info for " + packageName);
        try {
            ProjectContext context = contextService.detect();
            Map<String, String> deps = context.existingDeps();
            String installedRange = deps.get(packageName);

            JsonNode metadata = registryService.fetchPackageMetadata(packageName);
            if (metadata == null) {
                return Colors.red("Package '" + packageName + "' not found in registry.");
            }

            String name = metadata.path("name").asText(packageName);
            String description = metadata.path("description").asText("N/A");
            String latest = metadata.path("dist-tags").path("latest").asText("N/A");
            String license = metadata.path("license").asText("N/A");
            String homepage = metadata.path("homepage").asText("N/A");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s      %s%n", Colors.dim("Name:"), Colors.bold(name)));
            sb.append(String.format("%s %s%n", Colors.dim("Description:"), description));
            sb.append(String.format("%s      %s%n", Colors.dim("Latest:"), Colors.green(latest)));

            if (installedRange != null) {
                ResolutionResult result = resolverService.resolve(packageName, installedRange, context);
                String resolved = result.hasResolution() ? result.resolvedVersion() : Colors.yellow("could not resolve");
                sb.append(String.format("%s   %s %s %s%n", Colors.dim("Installed:"), Colors.yellow(installedRange), Colors.dim("\u2192"), Colors.bold(resolved)));
            } else {
                sb.append(String.format("%s   %s%n", Colors.dim("Installed:"), Colors.dim("not installed")));
            }

            sb.append(String.format("%s     %s%n", Colors.dim("License:"), license));
            sb.append(String.format("%s    %s", Colors.dim("Homepage:"), homepage));

            return sb.toString();
        } finally {
            Spinner.stop();
        }
    }
}
