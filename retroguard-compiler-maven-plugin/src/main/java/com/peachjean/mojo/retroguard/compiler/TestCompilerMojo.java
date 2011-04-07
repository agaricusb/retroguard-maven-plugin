package com.peachjean.mojo.retroguard.compiler;

import com.peachjean.mojo.retroguard.Utils;
import org.apache.maven.execution.MavenSession;

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
    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession localSession;

    @Override
    protected List<String> getClasspathElements() {
        return Utils.replaceWithUnobfuscated(super.getClasspathElements(), localSession);
    }
}
