package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.AddService;
import com.pkgfit.service.CompatibilityService;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstallCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private AddService addService;
    private RegistryService registryService;
    private CompatibilityService compatibilityService;
    private InstallCommands commands;
    private ProjectContext emptyContext;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        addService = mock(AddService.class);
        registryService = mock(RegistryService.class);
        compatibilityService = mock(CompatibilityService.class);
        mapper = new ObjectMapper();
        emptyContext = ProjectContext.empty();
        commands = new InstallCommands(contextService, resolverService, addService,
                registryService, compatibilityService);
    }

    @Test
    void batchUpdateAllDeps() {
        Map<String, String> deps = Map.of("lodash", "^4.0.0", "react", "^17.0.0");
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", deps, "Linux", "x64", true));
        when(resolverService.resolve(eq("lodash"), eq("^4.0.0"), any()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", List.of(), false));
        when(resolverService.resolve(eq("react"), eq("^17.0.0"), any()))
                .thenReturn(new ResolutionResult("react", "17.0.2", List.of(), false));

        String output = commands.install("", false);

        assertTrue(output.contains("updated"));
    }

    @Test
    void batchShowsUnchanged() {
        Map<String, String> deps = Map.of("lodash", "^4.18.1");
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", deps, "Linux", "x64", true));
        when(resolverService.resolve(eq("lodash"), eq("^4.18.1"), any()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", List.of(), false));

        String output = commands.install("", false);

        assertTrue(output.contains("unchanged"));
    }

    @Test
    void installSinglePackage() throws Exception {
        when(contextService.detect()).thenReturn(emptyContext);
        when(resolverService.resolve(eq("lodash"), eq(""), any()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", List.of(), false));
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata("lodash")).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq("4.18.1"), any()))
                .thenReturn("4.18.1");

        String output = commands.install("lodash", false);

        assertTrue(output.contains("Installed"));
    }

    @Test
    void installSinglePackageWithRange() throws Exception {
        when(contextService.detect()).thenReturn(emptyContext);
        when(resolverService.resolve(eq("react"), eq("^18.0.0"), any()))
                .thenReturn(new ResolutionResult("react", "18.3.1", List.of(), false));
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata("react")).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq("18.3.1"), any()))
                .thenReturn("18.3.1");

        String output = commands.install("react@^18.0.0", false);

        assertTrue(output.contains("Installed"));
    }

    @Test
    void installMultiplePackages() throws Exception {
        when(contextService.detect()).thenReturn(emptyContext);
        when(resolverService.resolve(eq("lodash"), eq(""), any()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", List.of(), false));
        when(resolverService.resolve(eq("react"), eq(""), any()))
                .thenReturn(new ResolutionResult("react", "18.3.1", List.of(), false));
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata("lodash")).thenReturn(metadata);
        when(registryService.fetchPackageMetadata("react")).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq("4.18.1"), any())).thenReturn("4.18.1");
        when(compatibilityService.findCompatibleVersion(any(), eq("18.3.1"), any())).thenReturn("18.3.1");

        String output = commands.install("lodash react", false);

        assertTrue(output.contains("installed"));
    }

    @Test
    void installReturnsErrorWhenResolutionFails() {
        when(resolverService.resolve(eq("unknown"), eq(""), any()))
                .thenReturn(new ResolutionResult("unknown", null, List.of(), false));

        String output = commands.install("unknown", false);

        assertTrue(output.contains("Could not resolve"));
    }

    @Test
    void installWithDevFlag() throws Exception {
        when(contextService.detect()).thenReturn(emptyContext);
        when(resolverService.resolve(eq("mocha"), eq(""), any()))
                .thenReturn(new ResolutionResult("mocha", "10.7.3", List.of(), false));
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata("mocha")).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq("10.7.3"), any())).thenReturn("10.7.3");

        String output = commands.install("mocha", true);

        assertTrue(output.contains("Installed"));
        verify(addService).addDependency(eq("mocha"), eq("^10.7.3"), eq(true), any(Path.class));
    }

    @Test
    void installFailsWhenNoCompatibleVersion() throws Exception {
        when(contextService.detect()).thenReturn(emptyContext);
        when(resolverService.resolve(eq("tailwindcss"), eq(""), any()))
                .thenReturn(new ResolutionResult("tailwindcss", "4.0.0", List.of(), false));
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata("tailwindcss")).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq("4.0.0"), any())).thenReturn(null);

        String output = commands.install("tailwindcss", false);

        assertTrue(output.contains("compatible with existing dependencies"));
    }
}
