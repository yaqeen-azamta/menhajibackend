package com.springboot.manhaji.support;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link MessageSource} for loading localized error messages
 * from {@code messages.properties}. Services should throw exceptions with
 * {@code messages.get("error.key", args)} instead of inlining string literals,
 * so translations are centralized in one place.
 */
@Component
@RequiredArgsConstructor
public class Messages {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}
