package com.peachjean.mojo.retroguard;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractCompilerMojo;
import org.apache.maven.plugin.CompilationFailureException;
import org.apache.maven.plugin.MojoExecutionException;

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
