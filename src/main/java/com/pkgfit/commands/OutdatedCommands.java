package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.Spinner;

@ShellComponent
public class OutdatedCommands {

    private final ContextService contextService;
    private final RegistryService registryService;

    public OutdatedCommands(ContextService contextService, RegistryService registryService) {
        this.contextService = contextService;
        this.registryService = registryService;
    }

    @ShellMethod(value="Check for outdated dependencies.", key={"outdated","outd"})
    public String outdated(
            @ShellOption(arity = 0, defaultValue = "false", help = "Check only devDependencies", value = "--dev") boolean dev){
        ProjectContext context = contextService.detect();

        if(!context.packageJsonExists()){
            return Colors.red("No package.json found in current directory.");
        }

        Map<String, String> deps;
        String label;
        if (dev) {
            deps = contextService.readDevDepsOnly();
            label = "devDependencies";
        } else {
            deps = context.existingDeps();
            label = "dependencies";
        }

        if(deps.isEmpty()){
            return Colors.yellow("No ") + Colors.bold(label) + Colors.yellow(" found in package.json.");
        }

        Spinner.start("Checking " + deps.size() + " packages for updates");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(Colors.bold("Outdated " + label)).append(":\n");
            sb.append(Colors.dim("------------------\n"));

            for(Map.Entry<String, String> entry : deps.entrySet()){
                String name = entry.getKey();
                String currentVersion = entry.getValue();

                JsonNode metadata = registryService.fetchPackageMetadata(name);
                if(metadata == null){
                    sb.append(Colors.red(name + ": not found in registry\n"));
                    continue;
                }

                String latestVersion = metadata.path("dist-tags").path("latest").asText(null);
                if(latestVersion == null){
                    sb.append(Colors.yellow(name + ": no 'latest' version in registry\n"));
                    continue;
                }

                if(!currentVersion.equals(latestVersion)){
                    sb.append(String.format("  %s  %s  %s  %s\n",
                            Colors.cyan(name),
                            Colors.yellow(currentVersion),
                            Colors.dim("\u2192"),
                            Colors.green(latestVersion)));
                }
            }

            return sb.toString();
        } finally {
            Spinner.stop();
        }
    }
}
