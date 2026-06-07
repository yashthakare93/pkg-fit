package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.AddService;
import com.pkgfit.service.CompatibilityService;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.NpmService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private AddService addService;
    private RegistryService registryService;
    private CompatibilityService compatibilityService;
    private NpmService npmService;
    private AddCommands commands;
    private ProjectContext emptyContext;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        addService = mock(AddService.class);
        registryService = mock(RegistryService.class);
        compatibilityService = mock(CompatibilityService.class);
        npmService = mock(NpmService.class);
        mapper = new ObjectMapper();
        emptyContext = ProjectContext.empty();
        when(contextService.detect()).thenReturn(emptyContext);
        commands = new AddCommands(contextService, resolverService, addService,
                registryService, compatibilityService, npmService);
    }

    private void mockCompatibility(String version) {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata(any())).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq(version), any())).thenReturn(version);
    }

    @Test
    void addWithoutRangeResolvesAndWritesPrefixedVersion() {
        ResolutionResult result = new ResolutionResult("lodash", "4.18.1", List.of(), false);
        when(resolverService.resolve("lodash", "", emptyContext)).thenReturn(result);
        mockCompatibility("4.18.1");

        String output = commands.add("lodash", false, false, false);

        assertEquals("Added lodash@^4.18.1 as dependency", Colors.strip(output));
    }

    @Test
    void addWithRangeWritesRangeAsIs() {
        ResolutionResult result = new ResolutionResult("react", "18.3.1", List.of(), false);
        when(resolverService.resolve("react", "^18.0.0", emptyContext)).thenReturn(result);
        mockCompatibility("18.3.1");

        String output = commands.add("react@^18.0.0", false, false, false);

        assertEquals("Added react@^18.0.0 as dependency", Colors.strip(output));
    }

    @Test
    void addWithExactVersionWritesExact() {
        ResolutionResult result = new ResolutionResult("react", "18.3.1", List.of(), false);
        when(resolverService.resolve("react", "18.3.1", emptyContext)).thenReturn(result);
        mockCompatibility("18.3.1");

        String output = commands.add("react@18.3.1", false, false, false);

        assertEquals("Added react@18.3.1 as dependency", Colors.strip(output));
    }

    @Test
    void addDevDependency() {
        ResolutionResult result = new ResolutionResult("mocha", "11.7.6", List.of(), false);
        when(resolverService.resolve("mocha", "", emptyContext)).thenReturn(result);
        mockCompatibility("11.7.6");

        String output = commands.add("mocha", true, false, false);

        assertEquals("Added mocha@^11.7.6 as devDependency", Colors.strip(output));
    }

    @Test
    void addScopedPackage() {
        ResolutionResult result = new ResolutionResult("@angular/core", "15.0.0", List.of(), false);
        when(resolverService.resolve("@angular/core", "15.0.0", emptyContext)).thenReturn(result);
        mockCompatibility("15.0.0");

        String output = commands.add("@angular/core@15.0.0", false, false, false);

        assertEquals("Added @angular/core@15.0.0 as dependency", Colors.strip(output));
    }

    @Test
    void addReturnsErrorWhenResolutionFails() {
        ResolutionResult result = new ResolutionResult("unknown", null, List.of(), false);
        when(resolverService.resolve("unknown", "", emptyContext)).thenReturn(result);

        String output = commands.add("unknown", false, false, false);

        assertEquals("Could not resolve 'unknown'.", Colors.strip(output));
    }

    @Test
    void addReturnsErrorWhenNoCompatibleVersion() {
        ResolutionResult result = new ResolutionResult("framer-motion", "11.0.0", List.of(), false);
        when(resolverService.resolve("framer-motion", "", emptyContext)).thenReturn(result);
        ObjectNode metadata = mapper.createObjectNode();
        metadata.set("versions", mapper.createObjectNode());
        when(registryService.fetchPackageMetadata("framer-motion")).thenReturn(metadata);
        when(compatibilityService.findCompatibleVersion(any(), eq("11.0.0"), any())).thenReturn(null);

        String output = commands.add("framer-motion", false, false, false);

        assertEquals("Could not find a version of 'framer-motion' compatible with existing dependencies.",
                Colors.strip(output));
    }

    @Test
    void addPinsExactVersion() {
        ResolutionResult result = new ResolutionResult("lodash", "4.18.1", List.of(), false);
        when(resolverService.resolve("lodash", "", emptyContext)).thenReturn(result);
        mockCompatibility("4.18.1");

        String output = commands.add("lodash", false, true, false);

        assertEquals("Added lodash@4.18.1 as dependency", Colors.strip(output));
    }

    @Test
    void addReturnsErrorOnIOException() throws Exception {
        ResolutionResult result = new ResolutionResult("lodash", "4.18.1", List.of(), false);
        when(resolverService.resolve("lodash", "", emptyContext)).thenReturn(result);
        mockCompatibility("4.18.1");
        doThrow(new IOException("Permission denied")).when(addService).addDependency(any(), any(), eq(false), any());

        String output = commands.add("lodash", false, false, false);

        assertEquals("Failed to write package.json: Permission denied", Colors.strip(output));
    }
}
