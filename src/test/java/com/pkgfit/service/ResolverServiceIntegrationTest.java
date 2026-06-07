package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.pkgfit.model.ProjectContext;
import com.pkgfit.model.ResolutionResult;

@SpringBootTest(properties = "spring.shell.interactive.enabled=false")
@Tag("integration")
class ResolverServiceIntegrationTest {

    @Autowired
    private ResolverService resolverService;

    @Test
    void resolveRealPackageWithRange() {
        ResolutionResult result = resolverService.resolve("lodash", "^4.0.0", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertNotNull(result.resolvedVersion());
    }

    @Test
    void resolveRealPackageWithoutRange() {
        ResolutionResult result = resolverService.resolve("react", "", ProjectContext.empty());

        assertTrue(result.hasResolution());
        assertNotNull(result.resolvedVersion());
    }

}
