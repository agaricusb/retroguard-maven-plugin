package com.peachjean.mojo.retroguard;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: jbunting
 * Date: 4/4/11
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObfuscateMojo extends AbstractMojo {


    private File inJar;
    private File outJar;
    private File obfuscationLog;
    private File config;

    /**
     * Should the obfuscated jar be deployed with the build?
     *
     * @parameter expression="${obfuscate.attach}" default-value="false"
     *
     * @since 2.2
     */

    private boolean attach;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * @component
     */
    private MavenProject project;


    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Obfuscator obfuscator = new MavenObfuscator(inJar, outJar, obfuscationLog, config, getLog(), project);
        obfuscator.obfuscate();

        if(attach)
        {
            this.projectHelper.attachArtifact(project, "jar", classifier, outJar);
        }
    }
}
