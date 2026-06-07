package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackageCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private PackageCommands commands;
    private ProjectContext emptyContext;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        emptyContext = ProjectContext.empty();
        when(contextService.detect()).thenReturn(emptyContext);
        commands = new PackageCommands(contextService, resolverService);
    }

    @Test
    void resolvePackageShouldReturnResolvedVersionText() {
        ResolutionResult result = new ResolutionResult("foo", "1.2.0", List.of(), false);
        when(resolverService.resolve("foo", "^1.0.0", emptyContext)).thenReturn(result);

        String output = commands.resolvePackage("foo", "^1.0.0");

        assertEquals("Resolved foo -> 1.2.0", Colors.strip(output));
    }

    @Test
    void resolvePackageShouldReturnFailureMessageWhenNoResolution() {
        ResolutionResult result = new ResolutionResult("foo", null, List.of(), false);
        when(resolverService.resolve("foo", "^1.0.0", emptyContext)).thenReturn(result);

        String output = commands.resolvePackage("foo", "^1.0.0");

        assertEquals("Could not resolve 'foo' for range '^1.0.0'.", Colors.strip(output));
    }

    @Test
    void resolvePackageShouldShowInstalledNote() {
        ResolutionResult result = new ResolutionResult("foo", "1.2.0", List.of(), true);
        when(resolverService.resolve("foo", "^1.0.0", emptyContext)).thenReturn(result);

        String output = commands.resolvePackage("foo", "^1.0.0");

        assertEquals("Resolved foo -> 1.2.0 (already installed)", Colors.strip(output));
    }

    @Test
    void resolvePackageShouldHandleBlankVersionRange() {
        ResolutionResult result = new ResolutionResult("foo", null, List.of(), false);
        when(resolverService.resolve(eq("foo"), eq(""), eq(emptyContext))).thenReturn(result);

        String output = commands.resolvePackage("foo", "");

        assertEquals("Could not resolve 'foo' for range 'any'.", Colors.strip(output));
    }
}
