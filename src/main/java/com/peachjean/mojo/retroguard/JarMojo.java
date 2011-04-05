package com.peachjean.mojo.retroguard;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.jar.AbstractJarMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Goal which echoes a string.
 *
 * @extendsPlugin jar
 * @goal jar
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class JarMojo extends org.apache.maven.plugin.jar.JarMojo {

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public JarMojo() {
    }

    public JarMojo(MavenProjectHelper projectHelper, MavenProject project) {
        this.projectHelper = projectHelper;
        this.project = project;
    }

    @Override
    public void execute() throws MojoExecutionException {

        Utils.propagatePrivateFields(this, AbstractJarMojo.class, "project");

        File jarFile = createArchive();

        String classifier = getClassifier();

        projectHelper.attachArtifact( project, Utils.UNOBFUSCATED_TYPE, classifier, jarFile );
    }

    @Override
    protected String getType() {
        return Utils.OBFUSCATED_TYPE;
    }

}
