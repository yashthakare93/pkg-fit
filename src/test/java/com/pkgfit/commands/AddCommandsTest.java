package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.AddService;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.ResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private AddService addService;
    private AddCommands commands;
    private ProjectContext emptyContext;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        addService = mock(AddService.class);
        emptyContext = ProjectContext.empty();
        when(contextService.detect()).thenReturn(emptyContext);
        commands = new AddCommands(contextService, resolverService, addService);
    }

    @Test
    void addWithoutRangeResolvesAndWritesPrefixedVersion() {
        ResolutionResult result = new ResolutionResult("lodash", "4.18.1", List.of(), false);
        when(resolverService.resolve("lodash", "", emptyContext)).thenReturn(result);

        String output = commands.add("lodash", false);

        assertEquals("Added dependency 'lodash@^4.18.1' as a dependency.", output);
    }

    @Test
    void addWithRangeWritesRangeAsIs() {
        ResolutionResult result = new ResolutionResult("react", "18.3.1", List.of(), false);
        when(resolverService.resolve("react", "^18.0.0", emptyContext)).thenReturn(result);

        String output = commands.add("react@^18.0.0", false);

        assertEquals("Added dependency 'react@^18.0.0' as a dependency.", output);
    }

    @Test
    void addWithExactVersionWritesExact() {
        String output = commands.add("react@18.3.1", false);

        assertEquals("Added dependency 'react@18.3.1' as a dependency.", output);
    }

    @Test
    void addDevDependency() {
        ResolutionResult result = new ResolutionResult("mocha", "11.7.6", List.of(), false);
        when(resolverService.resolve("mocha", "", emptyContext)).thenReturn(result);

        String output = commands.add("mocha", true);

        assertEquals("Added devDependency 'mocha@^11.7.6' as a devDependency.", output);
    }

    @Test
    void addScopedPackage() {
        String output = commands.add("@angular/core@15.0.0", false);

        assertEquals("Added dependency '@angular/core@15.0.0' as a dependency.", output);
    }

    @Test
    void addReturnsErrorWhenResolutionFails() {
        ResolutionResult result = new ResolutionResult("unknown", null, List.of(), false);
        when(resolverService.resolve("unknown", "", emptyContext)).thenReturn(result);

        String output = commands.add("unknown", false);

        assertEquals("Could not resolve 'unknown'.", output);
    }

    @Test
    void addReturnsErrorOnIOException() throws Exception {
        ResolutionResult result = new ResolutionResult("lodash", "4.18.1", List.of(), false);
        when(resolverService.resolve("lodash", "", emptyContext)).thenReturn(result);
        doThrow(new IOException("Permission denied")).when(addService).addDependency(any(), any(), eq(false), any());

        String output = commands.add("lodash", false);

        assertEquals("Failed to write package.json: Permission denied", output);
    }
}
