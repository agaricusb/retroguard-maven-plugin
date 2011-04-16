package com.digitalreasoning.mojo.retroguard;

public class ObfuscationConfigurationException extends RuntimeException {
    public ObfuscationConfigurationException(String message, Object ... args) {
        super(String.format(message, args));
    }

    public ObfuscationConfigurationException(String message, Throwable cause, Object ... args) {
        super(String.format(message, args), cause);
    }
}
