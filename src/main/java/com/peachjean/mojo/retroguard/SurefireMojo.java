package com.peachjean.mojo.retroguard;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.SurefirePlugin;

import java.util.List;
/**
 * @extendsPlugin surefire
 * @requiresDependencyResolution test
 * @goal test
 * @phase test
 * @threadSafe
 * @noinspection JavaDoc
 */
public class SurefireMojo extends SurefirePlugin {
    @Override
    public List generateTestClasspath() throws DependencyResolutionRequiredException, MojoExecutionException {
        return Utils.replaceWithUnobfuscated(super.generateTestClasspath(), getPluginContext());
    }
}
