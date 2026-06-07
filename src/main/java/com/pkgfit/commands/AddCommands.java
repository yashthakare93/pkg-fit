package com.pkgfit.commands;

import java.io.IOException;
import java.nio.file.Path;

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
public class AddCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final AddService addService;
    private final RegistryService registryService;
    private final CompatibilityService compatibilityService;

    public AddCommands(ContextService contextService, ResolverService resolverService,
            AddService addService, RegistryService registryService,
            CompatibilityService compatibilityService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.addService = addService;
        this.registryService = registryService;
        this.compatibilityService = compatibilityService;
    }

    @ShellMethod(value = "Add a dependency to package.json.", key = {"add", "a"})
    public String add(
            String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Add as devDependency", value = "--dev") boolean dev) {
        PackageName parsed = PackageName.parse(packageName);

        String rangeToUse = parsed.range().isEmpty() ? "" : parsed.range();
        ProjectContext context = contextService.detect();
        ResolutionResult result = resolverService.resolve(parsed.name(), rangeToUse, context);
        if (!result.hasResolution()) {
            return "Could not resolve '" + packageName + "'.";
        }

        JsonNode metadata = registryService.fetchPackageMetadata(parsed.name());
        String versionToUse = result.resolvedVersion();
        String compatNote = "";

        if (metadata != null) {
            String compatible = compatibilityService.findCompatibleVersion(
                    metadata, versionToUse, context);
            if (compatible == null) {
                return "Could not find a version of '" + parsed.name()
                        + "' compatible with existing dependencies.";
            }
            if (!compatible.equals(versionToUse)) {
                versionToUse = compatible;
                compatNote = " (auto-selected " + versionToUse + " for peer dep compatibility)";
            }
        }

        String rangeToWrite;
        if (parsed.range().isEmpty()) {
            rangeToWrite = "^" + versionToUse;
        } else {
            rangeToWrite = parsed.range();
        }

        try {
            addService.addDependency(parsed.name(), rangeToWrite, dev, Path.of("."));
            String label = dev ? "devDependency" : "dependency";
            return String.format("Added %s '%s@%s' as a %s.%s",
                    label, parsed.name(), rangeToWrite, label, compatNote);
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }
}
