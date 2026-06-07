package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(properties = "spring.shell.interactive.enabled=false")
@Tag("integration")
class RegistryServiceIntegrationTest {

    @Autowired
    private RegistryService registryService;

    @Test
    void fetchRealPackageMetadata() {
        JsonNode result = registryService.fetchPackageMetadata("lodash");

        assertNotNull(result);
        assertEquals("lodash", result.get("name").asText());
        assertTrue(result.has("versions"));
        assertTrue(result.has("dist-tags"));
        assertNotNull(result.get("dist-tags").get("latest"));
    }

    @Test
    void fetchNonExistentPackageReturnsNull() {
        JsonNode result = registryService.fetchPackageMetadata(
                "this-package-does-not-exist-xyz-12345");

        assertNull(result);
    }
}
