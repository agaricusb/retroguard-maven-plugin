package com.digitalreasoning.mojo.retroguard.obfuscator;

public class ObfuscationException extends RuntimeException {
    public ObfuscationException(String message, Object ... args) {
        super(String.format(message, args));
    }

    public ObfuscationException(String message, Throwable cause, Object ... args) {
        super(String.format(message, args), cause);
    }
}