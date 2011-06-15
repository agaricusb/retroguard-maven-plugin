package com.digitalreasoning.mojo.retroguard.mojo;

import com.digitalreasoning.mojo.retroguard.Utils;
import com.digitalreasoning.mojo.retroguard.obfuscator.MavenObfuscator;
import com.digitalreasoning.mojo.retroguard.obfuscator.ObfuscationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * @requiresDependencyResolution compile
 */
public abstract class AbstractObfuscateMojo extends AbstractMojo
{
	/**
	 * @component
	 */
	protected MavenProjectHelper projectHelper;
	/**
	 * The Maven project.
	 *
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;
	/**
	 * Directory containing the generated artifact.
	 *
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	protected File outputDirectory;
	/**
	 * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
	 *
	 * @parameter
	 */
	protected String classifier;
	/**
	 * The current build session instance. This is used for
	 * toolchain manager API calls.
	 *
	 * @parameter default-value="${session}"
	 * @required
	 * @readonly
	 */
	protected MavenSession session;
	/**
	 * The retroguard configuration.
	 *
	 * @parameter expression="${retroguard.config}" default-value="${project.basedir}/retroguard.conf"
	 * @required true
	 */
	private File config;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		long start = System.currentTimeMillis();
	    File jarFile = getInputJar();
	    File obfuscatedJarFile = getOutputJar();
	    File obfuscationLogFile = Utils.getArtifactFile(outputDirectory, getFinalName(), classifier, Utils.SPEC_EXTENSION);

	    try {
	        MavenObfuscator obfuscator = new MavenObfuscator(jarFile, obfuscatedJarFile, obfuscationLogFile, config, new File(this.outputDirectory, "obfuscation"), getLog(), project);
	        obfuscator.obfuscate();
	    } catch (ObfuscationException e) {
	        throw new MojoFailureException("Could not successfully obfuscate.", e);
	    }

		postProcessObfuscated(obfuscatedJarFile);

		projectHelper.attachArtifact( project, Utils.SPEC_TYPE, classifier, obfuscationLogFile);

		getLog().info("Obfuscation Complete: " + (System.currentTimeMillis() - start)/1000.0 + "s");
	}

	public abstract String getFinalName();

	protected abstract void postProcessObfuscated(File obfuscatedJarFile);

	protected abstract File getOutputJar();

	protected abstract File getInputJar();
}
