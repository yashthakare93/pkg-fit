package com.pkgfit.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.service.RegistryService;
import com.pkgfit.util.Colors;
import com.pkgfit.util.Spinner;

@ShellComponent
public class SearchCommands {
    
    private final RegistryService registryService;

    public SearchCommands(RegistryService registryService) {
        this.registryService = registryService;
    }

    @ShellMethod(value = "Search for packages.", key = "search")
    public String search(@ShellOption(help = "Search query") String query) {
        Spinner.start("Searching for \"" + query + "\"");
        try {
            JsonNode result = registryService.searchPackages(query);
            if(result == null){
                return Colors.red("Search failed.");
            }
            JsonNode objects = result.path("objects");
            if(!objects.isArray() || objects.size() == 0){
                return Colors.yellow("No packages found for query: ") + Colors.bold(query);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(Colors.bold("Search results")).append(Colors.dim(" for: ")).append(Colors.cyan(query)).append("\n");
            sb.append(Colors.dim("--------------------------------------------------\n"));
            for(JsonNode obj : objects){
                JsonNode name = obj.path("package").path("name");
                JsonNode version = obj.path("package").path("version");
                JsonNode description = obj.path("package").path("description");
                sb.append(String.format("  %s  %s  %s\n",
                        Colors.cyan(name.asText("N/A")),
                        Colors.yellow("v" + version.asText("N/A")),
                        Colors.dim(description.asText("No description"))));
            }
            return sb.toString();
        } finally {
            Spinner.stop();
        }
    }
    
}
