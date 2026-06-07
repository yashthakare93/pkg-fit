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
        String[] parts = packageName.split("[ ,]");
        if (parts.length > 1) {
            return removeMultiple(parts, dev);
        }
        return removeSingle(packageName, dev);
    }

    private String removeMultiple(String[] packages, boolean dev) {
        StringBuilder sb = new StringBuilder();
        int removed = 0;
        int notFound = 0;
        for (String pkg : packages) {
            try {
                boolean wasRemoved = removeService.removeDependency(pkg, dev, Path.of("."));
                if (wasRemoved) {
                    sb.append(String.format("  \u2713 %s%n", pkg));
                    removed++;
                } else {
                    sb.append(String.format("  \u2717 %s \u2014 not found%n", pkg));
                    notFound++;
                }
            } catch (IOException e) {
                sb.append(String.format("  \u2717 %s \u2014 failed: %s%n", pkg, e.getMessage()));
                notFound++;
            }
        }
        sb.append(String.format("\n%d removed, %d not found", removed, notFound));
        return sb.toString();
    }

    private String removeSingle(String packageName, boolean dev) {
        try {
            boolean removed = removeService.removeDependency(packageName, dev, Path.of("."));
            if (!removed) {
                return "Package '" + packageName + "' not found" + (dev ? " in devDependencies." : " in dependencies.");
            }
            String label = dev ? "devDependencies" : "dependencies";
            return "Removed '" + packageName + "' from " + label + ".";
        } catch (IOException e) {
            return "Failed to write package.json: " + e.getMessage();
        }
    }
}
