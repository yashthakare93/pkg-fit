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

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.AddService;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.NpmService;
import com.pkgfit.service.ResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private AddService addService;
    private NpmService npmService;
    private UpdateCommands commands;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        addService = mock(AddService.class);
        npmService = mock(NpmService.class);
        commands = new UpdateCommands(contextService, resolverService, addService, npmService);
    }

    @Test
    void updateExistingDep() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true));
        when(resolverService.resolve("lodash", "^4.0.0", contextService.detect()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", List.of(), false));

        String output = commands.update("lodash", false, false);

        assertTrue(output.contains("Updated"));
        assertTrue(output.contains("lodash"));
        assertTrue(output.contains("4.18.1"));
    }

    @Test
    void updateNonExistentDep() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));
        when(resolverService.resolve("lodash", "", contextService.detect()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", List.of(), false));

        String output = commands.update("lodash", false, false);

        assertTrue(output.contains("Updated"));
    }

    @Test
    void updateReturnsErrorWhenResolutionFails() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));
        when(resolverService.resolve("unknown", "", contextService.detect()))
                .thenReturn(new ResolutionResult("unknown", null, List.of(), false));

        String output = commands.update("unknown", false, false);

        assertTrue(output.contains("Could not resolve"));
    }

    @Test
    void updateWithCustomRange() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));
        when(resolverService.resolve("react", "^19.0.0", contextService.detect()))
                .thenReturn(new ResolutionResult("react", "19.2.7", List.of(), false));

        String output = commands.update("react@^19.0.0", false, false);

        assertTrue(output.contains("Updated"));
        assertTrue(output.contains("19.2.7"));
    }

    @Test
    void updateWithDevFlag() throws IOException {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("mocha", "^10.0.0"), "Linux", "x64", true));
        when(resolverService.resolve(eq("mocha"), eq("^10.0.0"), any()))
                .thenReturn(new ResolutionResult("mocha", "10.7.3", List.of(), false));

        String output = commands.update("mocha", true, false);

        assertTrue(output.contains("Updated"));
        verify(addService).addDependency(eq("mocha"), eq("^10.7.3"), eq(true), any(Path.class));
    }
}
