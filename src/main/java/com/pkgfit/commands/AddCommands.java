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
        String name;
        String range;

        if (packageName.startsWith("@")) {
            int atIndex = packageName.indexOf('@', 1);
            if (atIndex != -1) {
                name = packageName.substring(0, atIndex);
                range = packageName.substring(atIndex + 1);
            } else {
                name = packageName;
                range = "";
            }
        } else {
            int atIndex = packageName.indexOf('@');
            if (atIndex != -1) {
                name = packageName.substring(0, atIndex);
                range = packageName.substring(atIndex + 1);
            } else {
                name = packageName;
                range = "";
            }
        }

        String rangeToWrite;
        if (range.isEmpty()) {
            ProjectContext context = contextService.detect();
            ResolutionResult result = resolverService.resolve(name, "", context);
            if (!result.hasResolution()) {
                return "Could not resolve '" + name + "'.";
            }
            rangeToWrite = "^" + result.resolvedVersion();
        } else {
            rangeToWrite = range;
        }

        try {
            addService.addDependency(name, rangeToWrite, dev, Path.of("."));
            String label = dev ? "devDependency" : "dependency";
            return String.format("Added %s '%s@%s' as a %s.", label, name, rangeToWrite, label);
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }
}
