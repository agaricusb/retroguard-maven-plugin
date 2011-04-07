package com.peachjean.mojo.retroguard;

public class ObfuscationException extends Exception {
    public ObfuscationException(String message, Object ... args) {
        super(String.format(message, args));
    }

    public ObfuscationException(String message, Throwable cause, Object ... args) {
        super(String.format(message, args), cause);
    }
}
