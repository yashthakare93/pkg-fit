package com.pkgfit.commands;

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.PackageName;

@ShellComponent
public class InstallCommands {

    private final ContextService contextService;
    private final ResolverService resolverService;

    public InstallCommands(ContextService contextService, ResolverService resolverService) {
        this.contextService = contextService;
        this.resolverService = resolverService;
    }

    @ShellMethod(value="Resolve and show install plan for dependencies.", key={"install", "i"})
    public String install(@ShellOption(defaultValue="",help="Package name (optional)") String packageName){
            if(!packageName.isBlank()) {
                return resolveSingle(packageName);
            }
            return resolveAll();
    }

    private String resolveSingle(String input) {
        PackageName parsed = PackageName.parse(input);
        ProjectContext context = contextService.detect();
        ResolutionResult result = resolverService.resolve(parsed.name(), parsed.range(), context);

        if (!result.hasResolution()) {
            return "Could not resolve '" + input + "'.";
        }

        return String.format("npm install %s@%s", result.packageName(), result.resolvedVersion());
    }

    private String resolveAll(){
        ProjectContext context = contextService.detect();

        if(!context.packageJsonExists()){
            return "No package.json found in current directory.";
        }

        Map<String, String> deps = context.existingDeps();
        if(deps.isEmpty()){
            return "No dependencies found in package.json.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("npm install:\n");
        sb.append("------------------\n");


        for(Map.Entry<String, String> entry : deps.entrySet()){
            String name = entry.getKey();
            String versionRange = entry.getValue();

            ResolutionResult result = resolverService.resolve(name, versionRange, context);
            if(result.hasResolution()){
                sb.append(String.format("%s@%s\n", result.packageName(), result.resolvedVersion()));
            } else {
                sb.append(String.format("%s: could not resolve a version for range '%s'\n", name, versionRange));
            }
        }
        return sb.toString();
    }
        
        
}
