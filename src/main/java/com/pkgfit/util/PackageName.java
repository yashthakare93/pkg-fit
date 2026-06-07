package com.pkgfit.util;

public record PackageName(String name, String range) {

    public static PackageName parse(String input) {
        if (input == null || input.isBlank()) {
            return new PackageName("", "");
        }
        String name;
        String range;
        if (input.startsWith("@")) {
            int atIndex = input.indexOf('@', 1);
            if (atIndex != -1) {
                name = input.substring(0, atIndex);
                range = input.substring(atIndex + 1);
            } else {
                name = input;
                range = "";
            }
        } else {
            int atIndex = input.indexOf('@');
            if (atIndex != -1) {
                name = input.substring(0, atIndex);
                range = input.substring(atIndex + 1);
            } else {
                name = input;
                range = "";
            }
        }
        return new PackageName(name, range);
    }
}
