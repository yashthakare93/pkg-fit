package com.pkgfit.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class NpmService {

    public String install(Path workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("npm", "install");
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return "npm install completed successfully.";
            } else {
                return "npm install failed (exit " + exitCode + "):\n" + output;
            }
        } catch (Exception e) {
            return "Failed to run npm install: " + e.getMessage();
        }
    }
}
