package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompatibilityServiceTest {

    private ResolverService resolverService;
    private CompatibilityService service;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        resolverService = mock(ResolverService.class);
        service = new CompatibilityService(resolverService);
        mapper = new ObjectMapper();
    }

    @Test
    void returnsPreferredWhenNoPeerDeps() {
        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode versions = mapper.createObjectNode();
        ObjectNode v1 = mapper.createObjectNode();
        v1.put("version", "1.0.0");
        versions.set("1.0.0", v1);
        metadata.set("versions", versions);

        ProjectContext ctx = new ProjectContext("20.0.0", Map.of("react", "^18.0.0"), "Linux", "x64", true);

        String result = service.findCompatibleVersion(metadata, "1.0.0", ctx);

        assertEquals("1.0.0", result);
    }

    @Test
    void returnsPreferredWhenPeerDepsSatisfied() {
        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode versions = mapper.createObjectNode();
        ObjectNode v1 = mapper.createObjectNode();
        ObjectNode peerDeps = mapper.createObjectNode();
        peerDeps.put("react", "^18.0.0");
        v1.set("peerDependencies", peerDeps);
        versions.set("1.0.0", v1);
        metadata.set("versions", versions);

        when(resolverService.resolve(eq("react"), eq("^18.0.0"), any()))
                .thenReturn(new ResolutionResult("react", "18.3.1", List.of(), false));

        ProjectContext ctx = new ProjectContext("20.0.0", Map.of("react", "^18.0.0"), "Linux", "x64", true);

        String result = service.findCompatibleVersion(metadata, "1.0.0", ctx);

        assertEquals("1.0.0", result);
    }

    @Test
    void fallsBackWhenPeerDepsNotSatisfied() {
        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode versions = mapper.createObjectNode();

        ObjectNode v2 = mapper.createObjectNode();
        ObjectNode peerDepsV2 = mapper.createObjectNode();
        peerDepsV2.put("react", "^19.0.0");
        v2.set("peerDependencies", peerDepsV2);
        versions.set("2.0.0", v2);

        ObjectNode v1 = mapper.createObjectNode();
        ObjectNode peerDepsV1 = mapper.createObjectNode();
        peerDepsV1.put("react", "^18.0.0");
        v1.set("peerDependencies", peerDepsV1);
        versions.set("1.0.0", v1);

        metadata.set("versions", versions);

        when(resolverService.resolve(eq("react"), eq("^18.0.0"), any()))
                .thenReturn(new ResolutionResult("react", "18.3.1", List.of(), false));

        ProjectContext ctx = new ProjectContext("20.0.0", Map.of("react", "^18.0.0"), "Linux", "x64", true);

        String result = service.findCompatibleVersion(metadata, "2.0.0", ctx);

        assertEquals("1.0.0", result);
    }

    @Test
    void returnsNullWhenNoCompatibleVersion() {
        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode versions = mapper.createObjectNode();

        ObjectNode v1 = mapper.createObjectNode();
        ObjectNode peerDeps = mapper.createObjectNode();
        peerDeps.put("react", "^19.0.0");
        v1.set("peerDependencies", peerDeps);
        versions.set("1.0.0", v1);

        metadata.set("versions", versions);

        when(resolverService.resolve(eq("react"), eq("^18.0.0"), any()))
                .thenReturn(new ResolutionResult("react", "18.3.1", List.of(), false));

        ProjectContext ctx = new ProjectContext("20.0.0", Map.of("react", "^18.0.0"), "Linux", "x64", true);

        String result = service.findCompatibleVersion(metadata, "1.0.0", ctx);

        assertNull(result);
    }

    @Test
    void returnsPreferredWhenNoInstalledDeps() {
        ObjectNode metadata = mapper.createObjectNode();
        ObjectNode versions = mapper.createObjectNode();
        ObjectNode v1 = mapper.createObjectNode();
        ObjectNode peerDeps = mapper.createObjectNode();
        peerDeps.put("react", "^18.0.0");
        v1.set("peerDependencies", peerDeps);
        versions.set("1.0.0", v1);
        metadata.set("versions", versions);

        ProjectContext ctx = new ProjectContext("20.0.0", Map.of(), "Linux", "x64", false);

        String result = service.findCompatibleVersion(metadata, "1.0.0", ctx);

        assertEquals("1.0.0", result);
    }
}
