package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;
import com.pkgfit.util.Colors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DescribeCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private RegistryService registryService;
    private DescribeCommands commands;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        registryService = mock(RegistryService.class);
        mapper = new ObjectMapper();
        commands = new DescribeCommands(contextService, resolverService, registryService);
    }

    @Test
    void describeInstalledPackage() throws Exception {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true));

        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("name", "lodash");
        metadata.put("description", "Lodash modular utilities.");
        metadata.put("license", "MIT");
        metadata.put("homepage", "https://lodash.com/");
        ObjectNode distTags = mapper.createObjectNode();
        distTags.put("latest", "4.18.1");
        metadata.set("dist-tags", distTags);
        when(registryService.fetchPackageMetadata("lodash")).thenReturn(metadata);

        ResolutionResult result = new ResolutionResult("lodash", "4.18.1", List.of(), false);
        when(resolverService.resolve("lodash", "^4.0.0", contextService.detect())).thenReturn(result);

        String output = commands.describe("lodash");

        String clean = Colors.strip(output);
        assertTrue(clean.contains("lodash"));
        assertTrue(clean.contains("Lodash modular utilities."));
        assertTrue(clean.contains("4.18.1"));
        assertTrue(clean.contains("^4.0.0"));
        assertTrue(clean.contains("4.18.1"));
        assertTrue(clean.contains("MIT"));
        assertTrue(clean.contains("https://lodash.com/"));
    }

    @Test
    void describeNotInstalledPackage() throws Exception {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", true));

        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("name", "react");
        metadata.put("description", "A JavaScript library");
        metadata.put("license", "MIT");
        metadata.put("homepage", "https://react.dev/");
        ObjectNode distTags = mapper.createObjectNode();
        distTags.put("latest", "18.3.1");
        metadata.set("dist-tags", distTags);
        when(registryService.fetchPackageMetadata("react")).thenReturn(metadata);

        String output = commands.describe("react");

        String clean = Colors.strip(output);
        assertTrue(clean.contains("not installed"));
        assertTrue(clean.contains("18.3.1"));
    }

    @Test
    void describeUnknownPackage() throws Exception {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", true));
        when(registryService.fetchPackageMetadata("unknown")).thenReturn(null);

        String output = commands.describe("unknown");

        assertTrue(Colors.strip(output).contains("not found"));
    }
}
