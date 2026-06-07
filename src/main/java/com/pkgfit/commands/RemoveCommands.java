package com.pkgfit.commands;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.service.RemoveService;

@ShellComponent
public class RemoveCommands {

    private final RemoveService removeService;

    public RemoveCommands(RemoveService removeService) {
        this.removeService = removeService;
    }

    @ShellMethod(value = "Remove a dependency from package.json.", key = {"remove", "rm"})
    public String remove(
            String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Remove from devDependencies", value = "--dev") boolean dev) {
        try {
            boolean removed = removeService.removeDependency(packageName, dev, Path.of("."));
            if (!removed) {
                return "Package '" + packageName + "' not found" + (dev ? " in devDependencies." : " in dependencies.");
            }
            String label = dev ? "devDependency" : "dependency";
            return "Removed '" + packageName + "' from " + label + "s.";
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }
}
