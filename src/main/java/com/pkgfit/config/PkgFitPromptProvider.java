package com.pkgfit.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class PkgFitPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt() {
        return new AttributedString(" \u25C6 pkg-fit > ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold());
    }
}
