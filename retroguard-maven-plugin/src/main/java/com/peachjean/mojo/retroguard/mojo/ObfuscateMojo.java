package com.peachjean.mojo.retroguard.mojo;

import com.peachjean.mojo.retroguard.obfuscator.ObfuscationException;
import com.peachjean.mojo.retroguard.Utils;
import com.peachjean.mojo.retroguard.obfuscator.MavenObfuscator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.Collection;
import java.util.Map;

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

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * The retroguard configuration.
     *
     * @parameter expression="${retroguard.config}" default-value="${project.basedir}/retroguard.conf"
     * @required true
     */
    private File config;

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

        try {
            MavenObfuscator obfuscator = new MavenObfuscator(jarFile, obfuscatedJarFile, obfuscationLogFile, config, new File(this.outputDirectory, "obfuscation"), getLog(), project, session);
            obfuscator.setDependentSpecs(Utils.getObfuscationConfiguration(session).getDependencySpecs());
            obfuscator.setDependentJars(Utils.getObfuscationConfiguration(session).getUnobfuscatedMapping().values());
            obfuscator.obfuscate();
        } catch (ObfuscationException e) {
            throw new MojoFailureException("Could not successfully obfuscate.", e);
        }

        if ( classifier != null )
        {
            projectHelper.attachArtifact( project, "jar", classifier, obfuscatedJarFile );
        }
        else
        {
            project.getArtifact().setFile( obfuscatedJarFile );
        }

        projectHelper.attachArtifact( project, Utils.SPEC_TYPE, classifier, obfuscationLogFile);

    }
}
