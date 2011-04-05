package com.peachjean.mojo.retroguard;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Goal which echoes a string.
 *
 * @goal obfuscate
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class ObfuscateMojo extends AbstractMojo {

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" expression="${jar.finalName}" default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    public ObfuscateMojo() {
    }

    public ObfuscateMojo(MavenProjectHelper projectHelper, String finalName, MavenProject project, File outputDirectory, String classifier) {
        this.projectHelper = projectHelper;
        this.finalName = finalName;
        this.project = project;
        this.outputDirectory = outputDirectory;
        this.classifier = classifier;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File jarFile = Utils.getArtifactFile(outputDirectory, finalName, classifier, "jar");
        File obfuscatedJarFile = Utils.getArtifactFile(outputDirectory, finalName, classifier, Utils.OBFUSCATED_EXTENSION);
        File obfuscationLogFile = Utils.getArtifactFile(outputDirectory, finalName, classifier, Utils.SPEC_EXTENSION);

        // for each obfuscated dependency we need to get the spec file.  Then we need to check dates.  We will regenerate our spec file if ANY source files have changed
        File specFile = Utils.getArtifactFile(outputDirectory, finalName, classifier, Utils.GEN_SPEC_EXTENSION);

        MavenObfuscator obfuscator = new MavenObfuscator(jarFile, obfuscatedJarFile, obfuscationLogFile, specFile, getLog(), project);

        obfuscator.obfuscate();


        if ( classifier != null )
        {
            projectHelper.attachArtifact( project, Utils.OBFUSCATED_TYPE, classifier, obfuscatedJarFile );
        }
        else
        {
            project.getArtifact().setFile( obfuscatedJarFile );
        }

        projectHelper.attachArtifact( project, Utils.SPEC_TYPE, classifier, obfuscationLogFile);

    }
}
