package com.springboot.manhaji.support;

import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Builds a real {@link Messages} bean backed by the production
 * {@code messages.properties} file. Lets unit tests assert on the actual
 * localized strings without spinning up the Spring context.
 */
public final class TestMessages {

    private TestMessages() {}

    public static Messages create() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        return new Messages(source);
    }
}
