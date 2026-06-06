package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;

@ShellComponent
public class ListCommands {
    
    private final ContextService contextService;

    public ListCommands(ContextService contextService) {
        this.contextService = contextService;
    }

    @ShellMethod(value="List installed dependencies.", key={"list", "ls"})
    public String list() {
        ProjectContext context = contextService.detect();

        if(!context.packageJsonExists()){
            return "No package.json found in current directory.";
        }

        Map<String, String> deps = context.existingDeps();
        if(deps.isEmpty()){
            return "No dependencies found in package.json.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Dependencies (").append(deps.size()).append("):\n");
        sb.append("-----------------------------\n");


        deps.forEach((name, version) -> sb.append(String.format("%s@%s\n", name, version)));
        return sb.toString();
    }

}
