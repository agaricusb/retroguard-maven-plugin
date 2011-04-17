package com.digitalreasoning.mojo.retroguard.modifier;

import com.digitalreasoning.mojo.retroguard.ObfuscationConfiguration;
import com.digitalreasoning.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.digitalreasoning.mojo.retroguard.Utils;
import com.digitalreasoning.mojo.retroguard.obfuscator.ObfuscationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.war.util.MappingUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class WarObfuscationMojoExecutionModifier implements ObfuscationMojoExecutionModifier
{
	private static final String[] APPLICABLE_KEYS = new String[] { "org.apache.maven.plugins:maven-war-plugin" };

	@Override
	public String[] listApplicablePluginKeys()
	{
		return APPLICABLE_KEYS;
	}

	@Override
	public void modifyExecution(MavenSession session, MojoExecution mojoExecution, ObfuscationConfiguration configuration)
	{
		if(mojoExecution.getConfiguration().getChild("classifier") == null)
		{
		    Xpp3Dom classifier = new Xpp3Dom("classifier");
		    classifier.setValue(Utils.UNOBFUSCATED_CLASSIFIER);
		    mojoExecution.getConfiguration().addChild(classifier);
		}
		if(mojoExecution.getConfiguration().getChild("attachClasses") == null)
		{
			Xpp3Dom attach = new Xpp3Dom("attachClasses");
			attach.setValue(Boolean.TRUE.toString());
			mojoExecution.getConfiguration().addChild(attach);
		}
		else
		{
			mojoExecution.getConfiguration().getChild("attachClasses").setValue(Boolean.TRUE.toString());
		}

		// setup excludes for obfuscated libs

		List<Artifact> obfuscatedArtifacts = configuration.getObfuscatedArtifacts();
		String[] excludeParts = new String[obfuscatedArtifacts.size()];
		Xpp3Dom outputFileNameMappingConfig = mojoExecution.getConfiguration().getChild("outputFileNameMapping");
		String outputFileNameMapping = null;
		if(outputFileNameMappingConfig != null)
		{
			outputFileNameMapping = outputFileNameMappingConfig.getValue();
		}
		if(StringUtils.isBlank(outputFileNameMapping))
		{
			outputFileNameMapping = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";
		}
		try
		{
			for(int i = 0; i < obfuscatedArtifacts.size(); i++)
			{
				excludeParts[i] = "WEB-INF/lib/" + MappingUtils.evaluateFileNameMapping(outputFileNameMapping, obfuscatedArtifacts.get(i));
			}
		} catch (InterpolationException e)
		{
			throw new ObfuscationException("Couldn't create filename mappings successfully.", e);
		}

		Xpp3Dom excludesConfig = mojoExecution.getConfiguration().getChild("packagingExcludes");
		if(excludesConfig == null)
		{
			excludesConfig = new Xpp3Dom("packagingExcludes");
			mojoExecution.getConfiguration().addChild(excludesConfig);
		}
		String obfuscatedExcludes = StringUtils.join(excludeParts, ",");
		if(StringUtils.isBlank(excludesConfig.getValue()))
		{
			excludesConfig.setValue(obfuscatedExcludes);
		}
		else
		{
			excludesConfig.setValue(excludesConfig.getValue() + "," + obfuscatedExcludes);
		}

		// setup webResources for unobfuscated libs

		List<Artifact> unobfuscatedArtifacts = configuration.getUnobfuscatedArtifacts();
		Xpp3Dom resourceConfiguration = mojoExecution.getConfiguration().getChild("webResources");
		if(resourceConfiguration == null)
		{
			resourceConfiguration = new Xpp3Dom("webResources");
			mojoExecution.getConfiguration().addChild(resourceConfiguration);
		}
		for(Artifact unobfuscatedArtifact: unobfuscatedArtifacts)
		{
			Xpp3Dom resource = new Xpp3Dom(unobfuscatedArtifact.getArtifactId());
			Xpp3Dom directory = new Xpp3Dom("directory");
			directory.setValue(unobfuscatedArtifact.getFile().getParent());
			resource.addChild(directory);
			Xpp3Dom includes = new Xpp3Dom("includes");
			Xpp3Dom include = new Xpp3Dom("include");
			include.setValue(unobfuscatedArtifact.getFile().getName());
			includes.addChild(include);
			resource.addChild(includes);
			Xpp3Dom targetPath = new Xpp3Dom("targetPath");
			targetPath.setValue("WEB-INF/lib");
			resource.addChild(targetPath);
			resourceConfiguration.addChild(resource);
		}
	}
}
