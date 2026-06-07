package com.pkgfit.commands;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.AddService;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.PackageName;

@ShellComponent
public class UpdateCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final AddService addService;

    public UpdateCommands(ContextService contextService, ResolverService resolverService, AddService addService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.addService = addService;
    }

    @ShellMethod(value = "Update a dependency to latest version.", key = {"update", "up"})
    public String update(String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Update devDependency", value = "--dev") boolean dev) {
        PackageName parsed = PackageName.parse(packageName);

        if (parsed.name().isBlank()) {
            return "Package name is required.";
        }

        ProjectContext context = contextService.detect();
        String existingRange = context.existingDeps().get(parsed.name());
        String rangeToResolve = parsed.range().isEmpty() ? (existingRange != null ? existingRange : "") : parsed.range();

        ResolutionResult result = resolverService.resolve(parsed.name(), rangeToResolve, context);
        if (!result.hasResolution()) {
            return "Could not resolve '" + packageName + "'.";
        }

        String rangeToWrite = parsed.range().isEmpty()
                ? "^" + result.resolvedVersion()
                : parsed.range();

        try {
            addService.addDependency(parsed.name(), rangeToWrite, dev, Path.of("."));
            return String.format("Updated '%s' from '%s' to '%s' (resolved %s).",
                    parsed.name(), existingRange != null ? existingRange : "-", rangeToWrite, result.resolvedVersion());
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }
}
