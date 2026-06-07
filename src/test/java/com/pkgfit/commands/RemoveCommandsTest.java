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
import com.pkgfit.util.Colors;
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

        assertEquals("Removed lodash from dependencies.", Colors.strip(output));
    }

    @Test
    void removeNonExistentDep() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(false);

        String output = commands.remove("lodash", false);

        assertEquals("Package 'lodash' not found in dependencies.", Colors.strip(output));
    }

    @Test
    void removeDevDep() throws Exception {
        when(removeService.removeDependency(eq("mocha"), eq(true), any(Path.class))).thenReturn(true);

        String output = commands.remove("mocha", true);

        assertEquals("Removed mocha from devDependencies.", Colors.strip(output));
    }

    @Test
    void removeNonExistentDevDep() throws Exception {
        when(removeService.removeDependency(eq("mocha"), eq(true), any(Path.class))).thenReturn(false);

        String output = commands.remove("mocha", true);

        assertEquals("Package 'mocha' not found in devDependencies.", Colors.strip(output));
    }

    @Test
    void removeReturnsErrorOnIOException() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenThrow(new IOException("Permission denied"));

        String output = commands.remove("lodash", false);

        assertEquals("Failed to write package.json: Permission denied", Colors.strip(output));
    }

    @Test
    void removeMultiplePackages() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(true);
        when(removeService.removeDependency(eq("react"), eq(false), any(Path.class))).thenReturn(true);

        String output = commands.remove("lodash react", false);

        String clean = Colors.strip(output);
        assertTrue(clean.contains("\u2713 lodash"));
        assertTrue(clean.contains("\u2713 react"));
    }

    @Test
    void removeMultipleWithNotFound() throws Exception {
        when(removeService.removeDependency(eq("lodash"), eq(false), any(Path.class))).thenReturn(true);
        when(removeService.removeDependency(eq("unknown"), eq(false), any(Path.class))).thenReturn(false);

        String output = commands.remove("lodash unknown", false);

        String clean = Colors.strip(output);
        assertTrue(clean.contains("\u2713 lodash"));
        assertTrue(clean.contains("\u2717 unknown"));
    }
}
