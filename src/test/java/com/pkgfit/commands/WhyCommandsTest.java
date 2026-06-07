package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import com.pkgfit.model.ResolutionResult.SkippedVersion;
import com.pkgfit.service.ContextService;
import com.pkgfit.service.RegistryService;
import com.pkgfit.service.ResolverService;

class WhyCommandsTest {

    private ContextService contextService;
    private ResolverService resolverService;
    private RegistryService registryService;
    private WhyCommands commands;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        contextService = mock(ContextService.class);
        resolverService = mock(ResolverService.class);
        registryService = mock(RegistryService.class);
        mapper = new ObjectMapper();
        commands = new WhyCommands(contextService, resolverService, registryService);
    }

    @Test
    void whyShowsResolutionInfo() throws Exception {
        ProjectContext ctx = new ProjectContext("20.0.0", Map.of("lodash", "^4.0.0"), "Linux", "x64", true);
        when(contextService.detect()).thenReturn(ctx);

        List<SkippedVersion> skipped = List.of(
                new SkippedVersion("3.10.1", "does not satisfy range '^4.0.0'"),
                new SkippedVersion("4.0.0-alpha", "pre-release version not matched"));
        when(resolverService.resolve(eq("lodash"), eq("^4.0.0"), any()))
                .thenReturn(new ResolutionResult("lodash", "4.18.1", skipped, false));

        String metadataJson = """
                {
                    "name": "lodash",
                    "description": "Lodash",
                    "versions": {
                        "4.18.1": {
                            "peerDependencies": {}
                        }
                    }
                }
                """;
        when(registryService.fetchPackageMetadata("lodash"))
                .thenReturn(mapper.readTree(metadataJson));

        String output = commands.why("lodash");

        assertTrue(output.contains("lodash"));
        assertTrue(output.contains("Range:"));
        assertTrue(output.contains("^4.0.0"));
        assertTrue(output.contains("4.18.1"));
        assertTrue(output.contains("3.10.1"));
        assertTrue(output.contains("4.0.0-alpha"));
    }

    @Test
    void whyShowsPeerDeps() throws Exception {
        ProjectContext ctx = new ProjectContext("20.0.0", Map.of("react", "^18.0.0"), "Linux", "x64", true);
        when(contextService.detect()).thenReturn(ctx);

        when(resolverService.resolve(eq("react"), eq("^18.0.0"), any()))
                .thenReturn(new ResolutionResult("react", "18.3.1", List.of(), false));

        String metadataJson = """
                {
                    "name": "react",
                    "versions": {
                        "18.3.1": {
                            "peerDependencies": {
                                "react-dom": "^18.0.0"
                            }
                        }
                    }
                }
                """;
        when(registryService.fetchPackageMetadata("react"))
                .thenReturn(mapper.readTree(metadataJson));

        String output = commands.why("react");

        assertTrue(output.contains("react-dom"));
        assertTrue(output.contains("^18.0.0"));
    }

    @Test
    void whyNotFoundInRegistry() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of("unknown", "^1.0.0"), "Linux", "x64", true));
        when(registryService.fetchPackageMetadata("unknown")).thenReturn(null);

        String output = commands.why("unknown");

        assertTrue(output.contains("not found"));
    }

    @Test
    void whyNotInDeps() {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));
        when(registryService.fetchPackageMetadata("lodash")).thenReturn(mapper.createObjectNode());

        String output = commands.why("lodash");

        assertTrue(output.contains("not in your dependencies"));
    }

    @Test
    void whyWithExplicitRangeNotInstalled() throws Exception {
        when(contextService.detect()).thenReturn(new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false));

        List<SkippedVersion> skipped = List.of(
                new SkippedVersion("5.0.0-alpha", "pre-release version not matched"));
        when(resolverService.resolve(eq("lodash"), eq("^5.0.0"), any()))
                .thenReturn(new ResolutionResult("lodash", "5.1.0", skipped, false));

        String metadataJson = """
                {
                    "name": "lodash",
                    "versions": {
                        "5.1.0": {
                            "peerDependencies": {}
                        }
                    }
                }
                """;
        when(registryService.fetchPackageMetadata("lodash"))
                .thenReturn(mapper.readTree(metadataJson));

        String output = commands.why("lodash@^5.0.0");

        assertTrue(output.contains("lodash"));
        assertTrue(output.contains("5.1.0"));
        assertTrue(output.contains("5.0.0-alpha"));
    }

    @Test
    void whyEmptyPackageName() {
        String output = commands.why("");
        assertTrue(output.contains("Package name is required"));
    }
}
