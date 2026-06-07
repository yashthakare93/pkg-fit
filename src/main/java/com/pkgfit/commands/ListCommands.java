package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;
import com.pkgfit.util.Colors;

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
            return Colors.red("No package.json found in current directory.");
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
            return Colors.yellow("No ") + Colors.bold(label) + Colors.yellow(" found in package.json.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Colors.bold(label)).append(Colors.dim(" (" + deps.size() + ")")).append(":\n");
        sb.append(Colors.dim("-----------------------------\n"));

        deps.forEach((name, version) -> sb.append("  ").append(Colors.cyan(name)).append("@").append(Colors.yellow(version)).append("\n"));
        return sb.toString();
    }
}
