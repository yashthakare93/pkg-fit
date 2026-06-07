package com.pkgfit.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.Spinner;

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
        Spinner.start("Resolving " + packageName);
        try {
            String vr = versionRange == null || versionRange.isBlank() ? "" : versionRange;
            ProjectContext context = contextService.detect();
            ResolutionResult result = resolverService.resolve(packageName, vr, context);

            if (!result.hasResolution()) {
                return Colors.red("Could not resolve") + " '" + Colors.bold(packageName) + "' for range '" + Colors.yellow(vr.isEmpty() ? "any" : vr) + "'.";
            }

            String installedNote = result.isAlreadyInstalled() ? Colors.dim(" (already installed)") : "";
            return Colors.green("Resolved") + " " + Colors.cyan(packageName) + Colors.dim(" -> ") + Colors.bold(result.resolvedVersion()) + installedNote;
        } finally {
            Spinner.stop();
        }
    }
}
