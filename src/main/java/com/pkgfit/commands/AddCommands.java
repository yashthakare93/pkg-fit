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
public class AddCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final AddService addService;

    public AddCommands(ContextService contextService, ResolverService resolverService, AddService addService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.addService = addService;
    }

    @ShellMethod(value = "Add a dependency to package.json.", key = {"add", "a"})
    public String add(
            String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Add as devDependency", value = "--dev") boolean dev) {
        PackageName parsed = PackageName.parse(packageName);

        String rangeToWrite;
        if (parsed.range().isEmpty()) {
            ProjectContext context = contextService.detect();
            ResolutionResult result = resolverService.resolve(parsed.name(), "", context);
            if (!result.hasResolution()) {
                return "Could not resolve '" + parsed.name() + "'.";
            }
            rangeToWrite = "^" + result.resolvedVersion();
        } else {
            rangeToWrite = parsed.range();
        }

        try {
            addService.addDependency(parsed.name(), rangeToWrite, dev, Path.of("."));
            String label = dev ? "devDependency" : "dependency";
            return String.format("Added %s '%s@%s' as a %s.", label, parsed.name(), rangeToWrite, label);
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }
}
