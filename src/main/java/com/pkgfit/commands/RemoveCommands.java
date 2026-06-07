package com.pkgfit.commands;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.service.RemoveService;
import com.pkgfit.util.Colors;

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
                    sb.append("  ").append(Colors.green("\u2713")).append(" ").append(Colors.cyan(pkg)).append("\n");
                    removed++;
                } else {
                    sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(pkg)).append(" \u2014 ").append(Colors.yellow("not found")).append("\n");
                    notFound++;
                }
            } catch (IOException e) {
                sb.append("  ").append(Colors.red("\u2717")).append(" ").append(Colors.cyan(pkg)).append(" \u2014 ").append(Colors.red("failed: " + e.getMessage())).append("\n");
                notFound++;
            }
        }
        sb.append("\n").append(Colors.bold(removed + " removed")).append(", ").append(Colors.red(notFound + " not found"));
        return sb.toString();
    }

    private String removeSingle(String packageName, boolean dev) {
        try {
            boolean removed = removeService.removeDependency(packageName, dev, Path.of("."));
            if (!removed) {
                return Colors.yellow("Package '") + Colors.cyan(packageName) + Colors.yellow("' not found") + (dev ? " in devDependencies." : " in dependencies.");
            }
            String label = dev ? "devDependencies" : "dependencies";
            return Colors.green("Removed") + " " + Colors.cyan(packageName) + Colors.dim(" from " + label) + ".";
        } catch (IOException e) {
            return Colors.red("Failed to write package.json: " + e.getMessage());
        }
    }
}
