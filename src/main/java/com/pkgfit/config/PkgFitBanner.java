package com.pkgfit.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.pkgfit.util.Colors;

@Component
public class PkgFitBanner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.err.println();
        System.err.println("  " + Colors.bold(Colors.cyan("\u25C6 pkg-fit")) + Colors.dim(" v0.1.0") + "  " + Colors.dim("npm registry CLI"));
        System.err.println();
    }
}
