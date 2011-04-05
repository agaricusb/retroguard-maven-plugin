package com.peachjean.mojo.retroguard;

import java.util.List;

/**
 * Compiles application sources
 *
 * @extendsPlugin compiler
 * @goal compile
 * @phase compile
 * @threadSafe
 * @requiresDependencyResolution compile
 */
public class CompilerMojo extends org.apache.maven.plugin.CompilerMojo {

    @Override
    protected List<String> getClasspathElements() {
        return Utils.replaceWithUnobfuscated(super.getClasspathElements(), this.getPluginContext());
    }
}
