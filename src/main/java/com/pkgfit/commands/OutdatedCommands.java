package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;

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
            return "No package.json found in current directory.";
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
            return "No " + label + " found in package.json.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Outdated ").append(label).append(":\n");
        sb.append("------------------\n");

        for(Map.Entry<String, String> entry : deps.entrySet()){
            String name = entry.getKey();
            String currentVersion = entry.getValue();

            JsonNode metadata = registryService.fetchPackageMetadata(name);
            if(metadata == null){
                sb.append(String.format("%s: not found in registry\n", name));
                continue;
            }

            String latestVersion = metadata.path("dist-tags").path("latest").asText(null);
            if(latestVersion == null){
                sb.append(String.format("%s: no 'latest' version in registry\n", name));
                continue;
            }

            if(!currentVersion.equals(latestVersion)){
                sb.append(String.format("%s: %s -> %s\n", name, currentVersion, latestVersion));
            }
        }

        return sb.toString();
    }
}
