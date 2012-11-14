package com.digitalreasoning.mojo.retroguard.obfuscator;

public class ObfuscationException
extends RuntimeException {
    private static final long serialVersionUID = 2068944331594094636L;

	public ObfuscationException(String message, Object ... args) {
        super(String.format(message, args));
    }

    public ObfuscationException(String message, Throwable cause, Object ... args) {
        super(String.format(message, args), cause);
    }
}
