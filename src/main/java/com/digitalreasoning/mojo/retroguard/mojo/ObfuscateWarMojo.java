package com.digitalreasoning.mojo.retroguard.mojo;

import com.digitalreasoning.mojo.retroguard.ObfuscationConfiguration;
import com.digitalreasoning.mojo.retroguard.Utils;
import com.digitalreasoning.mojo.retroguard.obfuscator.ObfuscationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.war.util.MappingUtils;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal which obfuscates a war file.
 *
 * @goal warObfuscate
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
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
	 * The classifier to use for the attached classes artifact.
	 *
	 * @parameter default-value="classes"
	 * @since 2.1-alpha-2
	 */
	private String classesClassifier = "classes";

	/**
	 * To look up Archiver/UnArchiver implementations.
	 *
	 * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
	 * @required
	 */
	private ArchiverManager archiverManager;


	@Override
	public String getFinalName()
	{
		return finalName;
	}

	@Override
	protected void postProcessObfuscated(File obfuscatedJarFile)
	{
		File unobfuscatedWar = Utils.getArtifactFile(outputDirectory, finalName, Utils.UNOBFUSCATED_CLASSIFIER, "war");

		final ObfuscationConfiguration configuration = obfuscatedDependencyResolver.getObfuscationConfiguration(session);

		File extractDirectory = new File(outputDirectory, "obfuscated-war");

		File classesDir = new File(extractDirectory, "WEB-INF/classes");
		File obfuscatedWar = Utils.getArtifactFile(outputDirectory, finalName, null, "war");

		initStagingDirectory(unobfuscatedWar, extractDirectory, classesDir, configuration);

		addObfuscatedClassesToStaging(obfuscatedJarFile, classesDir);

		stageObfuscatedDependencies(configuration, extractDirectory);

		createObfuscatedWar(extractDirectory, obfuscatedWar);

		project.getArtifact().setFile(obfuscatedWar);
	}

	private void createObfuscatedWar(File extractDirectory, File obfuscatedWar)
	{
		try
		{
			WarArchiver warArchiver = (WarArchiver) archiverManager.getArchiver(obfuscatedWar);
			warArchiver.setIgnoreWebxml(false);
			warArchiver.setDestFile(obfuscatedWar);
			warArchiver.addDirectory(extractDirectory);
			warArchiver.createArchive();
		} catch (NoSuchArchiverException e)
		{
			throw new ObfuscationException("Could not find war archiver.", e);
		} catch (ArchiverException e)
		{
			throw new ObfuscationException("Failed to create obfuscated war file.", e);
		} catch (IOException e)
		{
			throw new ObfuscationException("Failed to create obfuscated war file.", e);
		}
	}

	private void stageObfuscatedDependencies(ObfuscationConfiguration configuration, File extractDirectory)
	{
		File libDir = new File(extractDirectory, "WEB-INF/lib");
		try
		{
			for(Artifact obfuscatedDependency: configuration.getObfuscatedArtifacts())
			{
				FileUtils.copyFileToDirectoryIfModified(obfuscatedDependency.getFile(), libDir);
			}
		} catch (IOException e)
		{
			throw new ObfuscationException("Could not stage obfuscated dependencies properly.", e);
		}
	}

	private void addObfuscatedClassesToStaging(File obfuscatedJarFile, File classesDir)
	{
		try
		{
			UnArchiver unArchiver = archiverManager.getUnArchiver(obfuscatedJarFile);
			unArchiver.setSourceFile(obfuscatedJarFile);
			unArchiver.setDestFile(classesDir);
			unArchiver.extract();
		} catch (NoSuchArchiverException e)
		{
			throw new ObfuscationException("Could not locate jar unarchiver.", e);
		} catch (ArchiverException e)
		{
			throw new ObfuscationException("Failed to extract obfuscated jar.", e);
		}
	}

	private void initStagingDirectory(File unobfuscatedWar, File extractDirectory, File classesDir, ObfuscationConfiguration configuration)
	{
		// create staging directory by unzipping the unobfuscated war
		extractDirectory.mkdirs();
		final List<String> mappedUnobfuscatedNames = new ArrayList<String>(configuration.getUnobfuscatedArtifacts().size());
		try
		{
			for(Artifact artifact: configuration.getUnobfuscatedArtifacts())
			{
				mappedUnobfuscatedNames.add(MappingUtils.evaluateFileNameMapping(outputFileNameMapping, artifact));
			}
		} catch (InterpolationException e)
		{
			throw new ObfuscationException("Could not determine mapped names of unobfuscated dependencies.", e);
		}
		try
		{
			UnArchiver unArchiver = archiverManager.getUnArchiver(unobfuscatedWar);
			unArchiver.setSourceFile(unobfuscatedWar);
			unArchiver.setDestFile(extractDirectory);
			unArchiver.setFileSelectors(new FileSelector[] {
					new FileSelector() {
						/**
						 * Exclude classes directory and unobfuscated jars.
						 */
						@Override
						public boolean isSelected(FileInfo fileInfo) throws IOException
						{
							if("WEB-INF/classes".equals(fileInfo.getName())) {
								return false;
							}
							if(fileInfo.getName().startsWith("WEB-INF/lib/")) {
								String name = fileInfo.getName().replace("WEB-INF/lib/","");
								if(mappedUnobfuscatedNames.contains(name))
								{
									return false;
								}
							}
							return true;
						}
					}
			});
			unArchiver.extract();
		} catch (NoSuchArchiverException e)
		{
			throw new ObfuscationException("Could not locate war unarchiver.", e);
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
		return Utils.getArtifactFile(outputDirectory, finalName, "obfuscated", "jar");
	}

	@Override
	protected File getInputJar()
	{
		return Utils.getArtifactFile(outputDirectory, finalName, classesClassifier, "jar");
	}
}
