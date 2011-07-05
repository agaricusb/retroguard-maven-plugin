package com.digitalreasoning.mojo.retroguard.mojo;

import com.digitalreasoning.mojo.retroguard.Utils;
import com.digitalreasoning.mojo.retroguard.obfuscator.ObfuscationException;
import com.google.common.base.Strings;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Goal which obfuscates a war file.
 *
 * @goal warObfuscate
 * @phase package
 * @requiresProject
 */

public class ObfuscateWarMojo extends AbstractObfuscateMojo
{
	/**
	 * The name of the generated WAR.
	 *
	 * @parameter default-value="${project.build.finalName}"
	 * @required
	 */
	private String finalName;

	private String outputFileNameMapping = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

	/**
	 * The WAR archiver.
	 *
	 * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="war"
	 */
	private WarArchiver warArchiver;

	/**
	 * @component role="org.codehaus.plexus.archiver.UnArchiver" roleHint="war"
	 */
	private UnArchiver warUnArchiver;

	/**
	 * @component role="org.codehaus.plexus.archiver.UnArchiver" roleHint="jar"
	 */
	private UnArchiver jarUnArchiver;

	/**
	 * Jar that we wish to obfuscate.
	 *
	 * @parameter
	 */
	protected File unobfuscatedJar;

	/**
	 * Classifier for the unobfuscated jar.  If unobfuscatedJar is specified, this will be ignored.
	 *
	 * @parameter default-value="classes"
	 */
	protected String unobfuscatedClassifier;


	@Override
	public String getFinalName()
	{
		return finalName;
	}

	@Override
	protected void postProcessObfuscated(File obfuscatedJarFile)
	{
		File unobfuscatedWar = Utils.getArtifactFile(outputDirectory, finalName, Utils.UNOBFUSCATED_CLASSIFIER, "war");

		File obfuscatedWar = Utils.getArtifactFile(outputDirectory, finalName, classifier, "war");

		if(!obfuscatedWar.exists()
				|| obfuscatedJarFile.lastModified() > obfuscatedWar.lastModified()
				|| unobfuscatedWar.lastModified() > obfuscatedWar.lastModified())
		{
			createObfuscatedWar(obfuscatedWar, unobfuscatedWar, obfuscatedJarFile);
		}
		else
		{
			getLog().info("Previously obfuscated war still current, not building obfuscated war...");
		}

		if ( classifier != null )
		{
			projectHelper.attachArtifact( project, "war", classifier, obfuscatedWar);
		    projectHelper.attachArtifact( project, "jar", classifier, obfuscatedJarFile );
		}
		else
		{
			project.getArtifact().setFile(obfuscatedWar);
			projectHelper.attachArtifact( project, "jar", obfuscatedJarFile );
		}
	}

	private void createObfuscatedWar(File obfuscatedWar, File unobfuscatedWar, File obfuscatedJar)
	{
		try
		{
			warArchiver.setIgnoreWebxml(false);
			warArchiver.setDestFile(obfuscatedWar);

			warArchiver.addArchivedFileSet(obfuscatedJar, "WEB-INF/classes/", new String[] { "**/*" }, new String[] { "META-INF/MANIFEST.MF" });
			warArchiver.addArchivedFileSet(unobfuscatedWar, new String[] { "**/*" }, new String [] { "WEB-INF/classes/**" });

			warArchiver.createArchive();
		} catch (ArchiverException e)
		{
			throw new ObfuscationException("Failed to create obfuscated war file.", e);
		} catch (IOException e)
		{
			throw new ObfuscationException("Failed to create obfuscated war file.", e);
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
		return unobfuscatedJar == null ? Utils.getArtifactFile(outputDirectory, finalName, unobfuscatedClassifier, "jar") : unobfuscatedJar;
	}

	@Override
	protected String getObfuscatedId() {
		return project.getId() + (Strings.isNullOrEmpty(classifier) ? "" : ":" + classifier);
	}

	@Override
	protected String getUnobfuscatedId() {
		return project.getId() + ":" + unobfuscatedClassifier;
	}
}
