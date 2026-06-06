package com.pkgfit.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.ResolverService;

@ShellComponent
public class PackageCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;

    public PackageCommands(ContextService contextService, ResolverService resolverService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
    }

    @ShellMethod(value = "Resolve the best matching npm package version.", key = {"resolve", "resolve-package"})
    public String resolvePackage(
            @ShellOption(help = "Name of the npm package to resolve") String packageName,
            @ShellOption(defaultValue = "", help = "Semver range to resolve against (e.g. ^1.2.0)") String versionRange) {
        ProjectContext context = contextService.detect();
        ResolutionResult result = resolverService.resolve(packageName, versionRange, context);

        if (!result.hasResolution()) {
            return String.format("Could not resolve package '%s' for version range '%s'.",
                    packageName,
                    versionRange == null || versionRange.isBlank() ? "(any)" : versionRange);
        }

        String installedNote = result.isAlreadyInstalled() ? " (already installed)" : "";
        return String.format("Resolved %s -> %s%s",
                packageName,
                result.resolvedVersion(),
                installedNote);
    }
}
