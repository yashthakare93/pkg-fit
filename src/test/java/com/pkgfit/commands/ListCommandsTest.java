package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;
import com.pkgfit.util.Colors;

class ListCommandsTest {

    private ContextService contextService;
    private ListCommands commands;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        commands = new ListCommands(contextService);
    }

    @Test
    void listShowsDeps() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true));

        String output = commands.list(false);

        String clean = Colors.strip(output);
        assertTrue(clean.contains("lodash"));
        assertTrue(clean.contains("^4.0.0"));
    }

    @Test
    void listNoPackageJson() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));

        String output = commands.list(false);

        assertTrue(Colors.strip(output).contains("No package.json"));
    }

    @Test
    void listEmptyDeps() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", true));

        String output = commands.list(false);

        assertTrue(Colors.strip(output).contains("No Dependencies"));
    }

    @Test
    void listDevDeps() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("mocha", "^10.0.0"), "Linux", "x64", true));
        when(contextService.readDevDepsOnly()).thenReturn(Map.of("mocha", "^10.0.0"));

        String output = commands.list(true);

        String clean = Colors.strip(output);
        assertTrue(clean.contains("mocha"));
        assertTrue(clean.contains("devDependencies"));
    }

    @Test
    void listDevDepsEmpty() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true));
        when(contextService.readDevDepsOnly()).thenReturn(Map.of());

        String output = commands.list(true);

        assertTrue(Colors.strip(output).contains("No devDependencies"));
    }
}
