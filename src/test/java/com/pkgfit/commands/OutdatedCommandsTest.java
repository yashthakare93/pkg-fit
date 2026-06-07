package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.util.Colors;

class OutdatedCommandsTest {

    private ContextService contextService;
    private RegistryService registryService;
    private OutdatedCommands commands;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        registryService = mock(RegistryService.class);
        mapper = new ObjectMapper();
        commands = new OutdatedCommands(contextService, registryService);
    }

    @Test
    void outdatedNoPackageJson() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));

        String output = commands.outdated(false);

        assertTrue(Colors.strip(output).contains("No package.json"));
    }

    @Test
    void outdatedEmptyDeps() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", true));

        String output = commands.outdated(false);

        assertTrue(Colors.strip(output).contains("No dependencies"));
    }

    @Test
    void outdatedShowsOutdated() throws Exception {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true));

        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode distTags = metadata.putObject("dist-tags");
        distTags.put("latest", "4.18.1");
        when(registryService.fetchPackageMetadata("lodash")).thenReturn(metadata);

        String output = commands.outdated(false);

        String clean = Colors.strip(output);
        assertTrue(clean.contains("lodash"));
        assertTrue(clean.contains("4.18.1"));
    }

    @Test
    void outdatedDevDeps() throws Exception {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("mocha", "^10.0.0"), "Linux", "x64", true));
        when(contextService.readDevDepsOnly()).thenReturn(Map.of("mocha", "^10.0.0"));

        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode distTags = metadata.putObject("dist-tags");
        distTags.put("latest", "11.7.6");
        when(registryService.fetchPackageMetadata("mocha")).thenReturn(metadata);

        String output = commands.outdated(true);

        String clean = Colors.strip(output);
        assertTrue(clean.contains("mocha"));
        assertTrue(clean.contains("devDependencies"));
    }

    @Test
    void outdatedDevDepsEmpty() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true));
        when(contextService.readDevDepsOnly()).thenReturn(Map.of());

        String output = commands.outdated(true);

        assertTrue(Colors.strip(output).contains("No devDependencies"));
    }
}
