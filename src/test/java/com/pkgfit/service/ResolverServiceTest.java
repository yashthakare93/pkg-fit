package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResolverServiceTest {

    private RegistryService registryService;
    private ResolverService resolverService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        registryService = mock(RegistryService.class);
        resolverService = new ResolverService(registryService);
        mapper = new ObjectMapper();
    }

    @Test
    void resolveShouldReturnHighestMatchingVersionForRange() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "1.2.0": {},
                    "2.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "^1.0.0", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertEquals("1.2.0", result.resolvedVersion());
        assertEquals("foo", result.packageName());
        assertFalse(result.isAlreadyInstalled());
    }

    @Test
    void resolveShouldReturnLatestWhenNoRangeSpecified() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "1.2.0": {},
                    "2.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertEquals("2.0.0", result.resolvedVersion());
    }

    @Test
    void resolveShouldReturnNoResolutionWhenMetadataIsNull() {
        when(registryService.fetchPackageMetadata("foo")).thenReturn(null);

        ResolutionResult result = resolverService.resolve("foo", "^1.0.0", ProjectContext.empty());

        assertFalse(result.hasResolution());
        assertEquals(null, result.resolvedVersion());
    }

    @Test
    void resolveShouldReturnNoResolutionWhenVersionsNodeIsNull() throws Exception {
        String json = "{}";

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "^1.0.0", ProjectContext.empty());

        assertFalse(result.hasResolution());
    }

    @Test
    void resolveShouldDetectAlreadyInstalled() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "1.2.0": {},
                    "2.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ProjectContext context = new ProjectContext("20.0.0", Map.of("foo", "1.2.0"), "Linux", "x64", true);
        ResolutionResult result = resolverService.resolve("foo", "^1.0.0", context);

        assertTrue(result.hasResolution());
        assertEquals("1.2.0", result.resolvedVersion());
        assertTrue(result.isAlreadyInstalled());
    }

    @Test
    void resolveShouldNotMarkInstalledWhenVersionIsOutsideRange() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "2.0.0": {},
                    "3.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ProjectContext context = new ProjectContext("20.0.0", Map.of("foo", "1.0.0"), "Linux", "x64", true);
        ResolutionResult result = resolverService.resolve("foo", "^2.0.0", context);

        assertTrue(result.hasResolution());
        assertEquals("2.0.0", result.resolvedVersion());
        assertFalse(result.isAlreadyInstalled());
    }

    @Test
    void resolveShouldReturnNoResolutionWhenNoVersionsMatchRange() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "1.2.0": {},
                    "1.9.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "^2.0.0", ProjectContext.empty());

        assertFalse(result.hasResolution());
    }

    @Test
    void resolveShouldPopulateSkippedVersionsForRangeMismatch() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "2.0.0": {},
                    "3.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "^1.0.0", ProjectContext.empty());

        assertEquals(2, result.skippedVersions().size());
        assertEquals("2.0.0", result.skippedVersions().get(0).version());
        assertEquals("3.0.0", result.skippedVersions().get(1).version());
        assertTrue(result.skippedVersions().get(0).reason().contains("does not satisfy range"));
    }

    @Test
    void resolveShouldPopulateSkippedVersionsForInvalidFormats() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0": {},
                    "not-a-version": {},
                    "2.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "", ProjectContext.empty());

        assertEquals("2.0.0", result.resolvedVersion());
        assertEquals(1, result.skippedVersions().size());
        assertEquals("not-a-version", result.skippedVersions().get(0).version());
        assertTrue(result.skippedVersions().get(0).reason().contains("invalid version format"));
    }

    @Test
    void resolveShouldHandleScopedPackageNames() throws Exception {
        String json = """
                {
                  "versions": {
                    "0.1.0": {},
                    "0.2.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("@scope/foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("@scope/foo", "", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertEquals("0.2.0", result.resolvedVersion());
        assertEquals("@scope/foo", result.packageName());
    }

    @Test
    void resolveShouldExcludePreReleaseWhenNoRangeSpecified() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0-alpha": {},
                    "1.0.0-beta": {},
                    "1.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertEquals("1.0.0", result.resolvedVersion());
    }

    @Test
    void resolveShouldExcludePreReleaseWhenRangeHasNoPreReleaseTag() throws Exception {
        String json = """
                {
                  "versions": {
                    "18.2.0": {},
                    "19.0.0-rc-fb9a90fa48-20240614": {},
                    "19.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("react")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("react", "^18.0.0", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertEquals("18.2.0", result.resolvedVersion());
    }

    @Test
    void resolveShouldIncludePreReleaseWhenRangeIncludesPreReleaseTag() throws Exception {
        String json = """
                {
                  "versions": {
                    "1.0.0-rc.1": {},
                    "1.0.0-rc.2": {},
                    "1.0.0": {}
                  }
                }
                """;

        when(registryService.fetchPackageMetadata("foo")).thenReturn(mapper.readTree(json));

        ResolutionResult result = resolverService.resolve("foo", "^1.0.0-rc", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertEquals("1.0.0-rc.2", result.resolvedVersion());
    }
}
