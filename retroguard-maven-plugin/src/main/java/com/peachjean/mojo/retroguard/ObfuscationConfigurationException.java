package com.peachjean.mojo.retroguard;

/**
 * Created by IntelliJ IDEA.
 * User: jbunting
 * Date: 4/8/11
 * Time: 9:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObfuscationConfigurationException extends RuntimeException {
    public ObfuscationConfigurationException(String message, Object ... args) {
        super(String.format(message, args));
    }

    public ObfuscationConfigurationException(String message, Throwable cause, Object ... args) {
        super(String.format(message, args), cause);
    }
}
