package com.springboot.manhaji.exception;

/**
 * Thrown when Gemini transcription fails with a retryable error (503, 429, timeout)
 * after all retry attempts are exhausted. Treated as a system failure — no
 * assessment result should be saved when this is thrown.
 */
public class SpeechServiceUnavailableException extends RuntimeException {
    public SpeechServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
