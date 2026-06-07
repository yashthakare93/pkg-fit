package com.pkgfit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PackageNameTest {

    @Test
    void parseSimpleName() {
        PackageName p = PackageName.parse("lodash");
        assertEquals("lodash", p.name());
        assertEquals("", p.range());
    }

    @Test
    void parseNameWithRange() {
        PackageName p = PackageName.parse("lodash@^4.0.0");
        assertEquals("lodash", p.name());
        assertEquals("^4.0.0", p.range());
    }

    @Test
    void parseNameWithExactVersion() {
        PackageName p = PackageName.parse("react@18.3.1");
        assertEquals("react", p.name());
        assertEquals("18.3.1", p.range());
    }

    @Test
    void parseScopedPackage() {
        PackageName p = PackageName.parse("@angular/core");
        assertEquals("@angular/core", p.name());
        assertEquals("", p.range());
    }

    @Test
    void parseScopedPackageWithRange() {
        PackageName p = PackageName.parse("@angular/core@15.0.0");
        assertEquals("@angular/core", p.name());
        assertEquals("15.0.0", p.range());
    }

    @Test
    void parseEmptyString() {
        PackageName p = PackageName.parse("");
        assertEquals("", p.name());
        assertEquals("", p.range());
    }

    @Test
    void parseNull() {
        PackageName p = PackageName.parse(null);
        assertEquals("", p.name());
        assertEquals("", p.range());
    }
}
