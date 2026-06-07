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
import com.pkgfit.service.NpmService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.PackageName;
import com.pkgfit.util.Spinner;

@ShellComponent
public class UpdateCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;
    private final AddService addService;
    private final NpmService npmService;

    public UpdateCommands(ContextService contextService, ResolverService resolverService, AddService addService, NpmService npmService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
        this.addService = addService;
        this.npmService = npmService;
    }

    @ShellMethod(value = "Update a dependency to latest version.", key = {"update", "up"})
    public String update(String packageName,
            @ShellOption(arity = 0, defaultValue = "false", help = "Update devDependency", value = "--dev") boolean dev,
            @ShellOption(arity = 0, defaultValue = "false", help = "Run npm install after", value = "--install") boolean install) {
        PackageName parsed = PackageName.parse(packageName);

        if (parsed.name().isBlank()) {
            return "Package name is required.";
        }

        Spinner.start("Resolving " + parsed.name());
        try {
            ProjectContext context = contextService.detect();
            String existingRange = context.existingDeps().get(parsed.name());
            String rangeToResolve = parsed.range().isEmpty() ? (existingRange != null ? existingRange : "") : parsed.range();

            ResolutionResult result = resolverService.resolve(parsed.name(), rangeToResolve, context);
            if (!result.hasResolution()) {
                return Colors.red("Could not resolve '" + packageName + "'.");
            }

            String rangeToWrite = parsed.range().isEmpty()
                    ? "^" + result.resolvedVersion()
                    : parsed.range();

            try {
                addService.addDependency(parsed.name(), rangeToWrite, dev, Path.of("."));
                String out = Colors.green("Updated") + " " + Colors.cyan(parsed.name())
                        + Colors.dim(" from ") + Colors.yellow(existingRange != null ? existingRange : "-")
                        + Colors.dim(" to ") + Colors.bold(rangeToWrite)
                        + Colors.dim(" (resolved " + result.resolvedVersion() + ").");
                if (install) {
                    out += "\n" + npmService.install(Path.of("."));
                }
                return out;
            } catch (IOException e) {
                return Colors.red("Failed to write package.json: " + e.getMessage());
            }
        } finally {
            Spinner.stop();
        }
    }
}
