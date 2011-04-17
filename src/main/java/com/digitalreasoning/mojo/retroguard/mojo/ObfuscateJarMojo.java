package com.digitalreasoning.mojo.retroguard.mojo;

import com.digitalreasoning.mojo.retroguard.obfuscator.ObfuscationException;
import com.digitalreasoning.mojo.retroguard.Utils;
import com.digitalreasoning.mojo.retroguard.obfuscator.MavenObfuscator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Goal which obfuscates a jar file.
 *
 * @goal jarObfuscate
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class ObfuscateJarMojo extends AbstractObfuscateMojo
{

	/**
	 * Name of the generated JAR.
	 *
	 * @parameter alias="jarName" expression="${jar.finalName}" default-value="${project.build.finalName}"
	 * @required
	 */
	protected String finalName;

	public ObfuscateJarMojo() {
    }

    public ObfuscateJarMojo(MavenProjectHelper projectHelper, String finalName, MavenProject project, File outputDirectory, String classifier) {
        this.projectHelper = projectHelper;
        this.finalName = finalName;
        this.project = project;
        this.outputDirectory = outputDirectory;
        this.classifier = classifier;
    }

	public String getFinalName()
	{
		return finalName;
	}

	@Override
	protected void postProcessObfuscated(File obfuscatedJarFile)
	{
		if ( classifier != null )
		{
		    projectHelper.attachArtifact( project, "jar", classifier, obfuscatedJarFile );
		}
		else
		{
		    project.getArtifact().setFile( obfuscatedJarFile );
		}
	}

	@Override
	protected File getOutputJar()
	{
		return Utils.getArtifactFile(outputDirectory, finalName, classifier, "jar");
	}

	@Override
	protected File getInputJar()
	{
		return Utils.getArtifactFile(outputDirectory, finalName, Utils.UNOBFUSCATED_CLASSIFIER, "jar");
	}
}
