package com.pkgfit.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.service.RegistryService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.Spinner;

@ShellComponent
public class InfoCommands {

    private final RegistryService registryService;

    public InfoCommands(RegistryService registryService) {
        this.registryService = registryService;
    }

    @ShellMethod(value = "Show package info from registry.", key = "info")
    public String info(@ShellOption(help = "Package name") String packageName) {
        Spinner.start("Fetching info for " + packageName);
        try {
            JsonNode metadata = registryService.fetchPackageMetadata(packageName);
            if (metadata == null) {
                return Colors.red("Package '" + packageName + "' not found in registry.");
            }

            String name = metadata.path("name").asText(packageName);
            String description = metadata.path("description").asText("N/A");
            String latest = metadata.path("dist-tags").path("latest").asText("N/A");
            String license = metadata.path("license").asText("N/A");
            String homepage = metadata.path("homepage").asText("N/A");

            return String.format("""
                    %s      %s
                    %s %s
                    %s      %s
                    %s     %s
                    %s    %s""",
                    Colors.dim("Name:"), Colors.bold(name),
                    Colors.dim("Description:"), description,
                    Colors.dim("Latest:"), Colors.green(latest),
                    Colors.dim("License:"), license,
                    Colors.dim("Homepage:"), homepage);
        } finally {
            Spinner.stop();
        }
    }
}
