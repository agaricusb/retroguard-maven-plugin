package com.peachjean.mojo.retroguard;

import java.util.List;

/**
 * Compiles test sources
 *
 * @extendsPlugin compiler
 * @goal testCompile
 * @phase test-compile
 * @threadSafe
 * @requiresDependencyResolution test
 */
public class TestCompilerMojo extends org.apache.maven.plugin.TestCompilerMojo {
    @Override
    protected List<String> getClasspathElements() {
        return Utils.replaceWithUnobfuscated(super.getClasspathElements(), this.getPluginContext());
    }
}
