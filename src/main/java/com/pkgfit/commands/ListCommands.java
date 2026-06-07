package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;

@ShellComponent
public class ListCommands {

    private final ContextService contextService;

    public ListCommands(ContextService contextService) {
        this.contextService = contextService;
    }

    @ShellMethod(value="List installed dependencies.", key={"list", "ls"})
    public String list(
            @ShellOption(arity = 0, defaultValue = "false", help = "Show only devDependencies", value = "--dev") boolean dev) {
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
            label = "Dependencies";
        }

        if(deps.isEmpty()){
            return "No " + label + " found in package.json.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" (").append(deps.size()).append("):\n");
        sb.append("-----------------------------\n");

        deps.forEach((name, version) -> sb.append(String.format("%s@%s\n", name, version)));
        return sb.toString();
    }
}
