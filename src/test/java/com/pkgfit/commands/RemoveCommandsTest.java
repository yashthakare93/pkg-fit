package com.pkgfit.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import com.pkgfit.service.RemoveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveCommandsTest {

    private RemoveService removeService;
    private RemoveCommands commands;

    @BeforeEach
    void setUp() {
        removeService = mock(RemoveService.class);
        commands = new RemoveCommands(removeService);
    }

    @Test
    void removeExistingDep() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(true);

        String output = commands.remove("lodash", false);

        assertEquals("Removed 'lodash' from dependencies.", output);
    }

    @Test
    void removeNonExistentDep() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(false);

        String output = commands.remove("lodash", false);

        assertEquals("Package 'lodash' not found in dependencies.", output);
    }

    @Test
    void removeDevDep() throws Exception {
        when(removeService.removeDependency(eq("mocha"), eq(true), any(Path.class))).thenReturn(true);

        String output = commands.remove("mocha", true);

        assertEquals("Removed 'mocha' from devDependencies.", output);
    }

    @Test
    void removeNonExistentDevDep() throws Exception {
        when(removeService.removeDependency(eq("mocha"), eq(true), any(Path.class))).thenReturn(false);

        String output = commands.remove("mocha", true);

        assertEquals("Package 'mocha' not found in devDependencies.", output);
    }

    @Test
    void removeReturnsErrorOnIOException() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenThrow(new IOException("Permission denied"));

        String output = commands.remove("lodash", false);

        assertEquals("Failed to write package.json: Permission denied", output);
    }

    @Test
    void removeMultiplePackages() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(true);
        when(removeService.removeDependency(eq("react"), eq(false), any(Path.class))).thenReturn(true);

        String output = commands.remove("lodash react", false);

        assertTrue(output.contains("\u2713 lodash"));
        assertTrue(output.contains("\u2713 react"));
    }

    @Test
    void removeMultipleWithNotFound() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(true);
        when(removeService.removeDependency(eq("unknown"), eq(false), any(Path.class))).thenReturn(false);

        String output = commands.remove("lodash unknown", false);

        assertTrue(output.contains("\u2713 lodash"));
        assertTrue(output.contains("\u2717 unknown"));
    }
}
