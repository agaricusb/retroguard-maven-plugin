package com.digitalreasoning.mojo.retroguard.mojo;

import com.digitalreasoning.mojo.retroguard.Utils;
import com.digitalreasoning.mojo.retroguard.obfuscator.ObfuscationException;
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
	 * @parameter
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

		File extractDirectory = new File(outputDirectory, "obfuscated-war");

		File classesDir = new File(extractDirectory, "WEB-INF/classes");
		File obfuscatedWar = Utils.getArtifactFile(outputDirectory, finalName, null, "war");

		if(!obfuscatedWar.exists()
				|| obfuscatedJarFile.lastModified() > obfuscatedWar.lastModified()
				|| unobfuscatedWar.lastModified() > obfuscatedWar.lastModified())
		{
			initStagingDirectory(unobfuscatedWar, extractDirectory, classesDir);

			addObfuscatedClassesToStaging(obfuscatedJarFile, classesDir);

			createObfuscatedWar(extractDirectory, obfuscatedWar);
		}
		else
		{
			getLog().info("Previously obfuscated war still current, not building obfuscated war...");
		}

		project.getArtifact().setFile(obfuscatedWar);
	}

	private void createObfuscatedWar(File extractDirectory, File obfuscatedWar)
	{
		try
		{
			warArchiver.setIgnoreWebxml(false);
			warArchiver.setDestFile(obfuscatedWar);
			warArchiver.addDirectory(extractDirectory);
//			use this!
//			warArchiver.addArchivedFileSet();
			warArchiver.createArchive();
		} catch (ArchiverException e)
		{
			throw new ObfuscationException("Failed to create obfuscated war file.", e);
		} catch (IOException e)
		{
			throw new ObfuscationException("Failed to create obfuscated war file.", e);
		}
	}

	private void addObfuscatedClassesToStaging(File obfuscatedJarFile, File classesDir)
	{
		try
		{
			jarUnArchiver.setSourceFile(obfuscatedJarFile);
			jarUnArchiver.setDestFile(classesDir);
			jarUnArchiver.extract();
		} catch (ArchiverException e)
		{
			throw new ObfuscationException("Failed to extract obfuscated jar.", e);
		}
	}

	private void initStagingDirectory(File unobfuscatedWar, File extractDirectory, File classesDir)
	{
		// create staging directory by unzipping the unobfuscated war
		extractDirectory.mkdirs();
		try
		{
			warUnArchiver.setSourceFile(unobfuscatedWar);
			warUnArchiver.setDestFile(extractDirectory);
			warUnArchiver.setFileSelectors(new FileSelector[]{
					new FileSelector()
					{
						/**
						 * Exclude classes directory and unobfuscated jars.
						 */
						@Override
						public boolean isSelected(FileInfo fileInfo) throws IOException
						{
							if ("WEB-INF/classes".equals(fileInfo.getName()))
							{
								return false;
							}
							return true;
						}
					}
			});
			warUnArchiver.extract();
		} catch (ArchiverException e)
		{
			throw new ObfuscationException("Failed to extract unobfuscated war.", e);
		}
		try
		{
			FileUtils.deleteDirectory(classesDir);
			classesDir.mkdirs();
		} catch (IOException e)
		{
			throw new ObfuscationException("Could not delete unobfuscated classes directory.", e);
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
}
