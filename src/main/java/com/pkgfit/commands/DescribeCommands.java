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
        ProjectContext context = contextService.detect();
        Map<String, String> deps = context.existingDeps();
        String installedRange = deps.get(packageName);

        JsonNode metadata = registryService.fetchPackageMetadata(packageName);
        if (metadata == null) {
            return "Package '" + packageName + "' not found in registry.";
        }

        String name = metadata.path("name").asText(packageName);
        String description = metadata.path("description").asText("N/A");
        String latest = metadata.path("dist-tags").path("latest").asText("N/A");
        String license = metadata.path("license").asText("N/A");
        String homepage = metadata.path("homepage").asText("N/A");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Name:        %s%n", name));
        sb.append(String.format("Description: %s%n", description));
        sb.append(String.format("Latest:      %s%n", latest));

        if (installedRange != null) {
            ResolutionResult result = resolverService.resolve(packageName, installedRange, context);
            String resolved = result.hasResolution() ? result.resolvedVersion() : "could not resolve";
            sb.append(String.format("Installed:   %s -> %s%n", installedRange, resolved));
        } else {
            sb.append("Installed:   not installed\n");
        }

        sb.append(String.format("License:     %s%n", license));
        sb.append(String.format("Homepage:    %s", homepage));

        return sb.toString();
    }
}
