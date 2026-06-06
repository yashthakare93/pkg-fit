package com.pkgfit.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.pkgfit.service.RegistryService;

@ShellComponent
public class SearchCommands {
    
    private final RegistryService registryService;

    public SearchCommands(RegistryService registryService) {
        this.registryService = registryService;
    }

    @ShellMethod(value = "Search for packages.", key = "search")
    public String search(@ShellOption(help = "Search query") String query) {
        
        JsonNode result = registryService.searchPackages(query);
        if(result == null){
            return "Search failed.";
        }
        JsonNode objects = result.path("objects");
        if(!objects.isArray() || objects.size() == 0){
            return "No packages found for query: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Search results for query: '").append(query).append("'\n");
        sb.append("--------------------------------------------------\n");
        for(JsonNode obj : objects){
            JsonNode name = obj.path("package").path("name");
            JsonNode version = obj.path("package").path("version");
            JsonNode description = obj.path("package").path("description");
            sb.append(String.format("%s@%s - %s\n", name.asText("N/A"), version.asText("N/A"), description.asText("No description")));
        }
        return sb.toString();
    }
    
}
