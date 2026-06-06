package com.pkgfit.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.service.RegistryService;

@ShellComponent
public class InfoCommands {

    private final RegistryService registryService;

    public InfoCommands(RegistryService registryService) {
        this.registryService = registryService;
    }

    @ShellMethod(value = "Show package info from registry.", key = "info")
    public String info(@ShellOption(help = "Package name") String packageName) {
        JsonNode metadata = registryService.fetchPackageMetadata(packageName);
        if (metadata == null) {
            return "Package '" + packageName + "' not found in registry.";
        }

        String name = metadata.path("name").asText(packageName);
        String description = metadata.path("description").asText("N/A");
        String latest = metadata.path("dist-tags").path("latest").asText("N/A");
        String license = metadata.path("license").asText("N/A");
        String homepage = metadata.path("homepage").asText("N/A");

        return String.format("""
                Name:        %s
                Description: %s
                Latest:      %s
                License:     %s
                Homepage:    %s""",
                name, description, latest, license, homepage);
    }
}
