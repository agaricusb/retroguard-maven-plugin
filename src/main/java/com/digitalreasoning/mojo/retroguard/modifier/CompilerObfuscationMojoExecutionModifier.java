package com.digitalreasoning.mojo.retroguard.modifier;

import com.google.common.base.Function;
import com.digitalreasoning.mojo.retroguard.ObfuscationConfiguration;
import com.digitalreasoning.mojo.retroguard.ObfuscationConfigurationException;
import com.digitalreasoning.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.digitalreasoning.mojo.retroguard.Utils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompilerObfuscationMojoExecutionModifier implements ObfuscationMojoExecutionModifier
{
	private static final String[] APPLICABLE_KEYS = new String[] { "org.apache.maven.plugins:maven-compiler-plugin" };

	@Override
	public String[] listApplicablePluginKeys()
	{
		return APPLICABLE_KEYS;
	}

	@Override
    public void modifyExecution(MavenSession session, MojoExecution mojoExecution, final ObfuscationConfiguration configuration) {
		List<String> classpathElements;
		try {
			Xpp3Dom classpathElementConfig = mojoExecution.getConfiguration().getChild("classpathElements");
			if(classpathElementConfig != null && classpathElementConfig.getChildren().length > 0)
			{
				Xpp3Dom[] children = classpathElementConfig.getChildren();
				classpathElements = new ArrayList<String>(children.length);
				for(Xpp3Dom child: children)
				{
					classpathElements.add(child.getValue());
				}

			}
			else if("compile".equals(mojoExecution.getGoal()))
			{
				classpathElements = session.getCurrentProject().getCompileClasspathElements();
			}
			else if("testCompile".equals(mojoExecution.getGoal()))
			{
				classpathElements = session.getCurrentProject().getTestClasspathElements();
			}
			else
			{
				return;
			}
			if(classpathElementConfig != null)
			{
				Xpp3Dom[] children = mojoExecution.getConfiguration().getChildren();
				for(int i = 0; i < children.length; i++)
				{
					if(children[i] == classpathElementConfig)
					{
						mojoExecution.getConfiguration().removeChild(i);
						break;
					}
				}
			}
			classpathElementConfig = new Xpp3Dom("classpathElements");
			mojoExecution.getConfiguration().addChild(classpathElementConfig);
			final Map<String,File> unobfuscatedMapping = configuration.getUnobfuscatedMapping();
			Utils.augmentConfigurationList(mojoExecution.getConfiguration(), "classpathElements", classpathElements, new Function<String, String>() {
				@Override
				public String apply(String input)
				{
					File mappedFile = unobfuscatedMapping.get(input);
					return mappedFile != null ? mappedFile.getPath() : input;
				}
			});
//			mojoExecution.getConfiguration().getChild("classpathElements").
		} catch (DependencyResolutionRequiredException e) {
			throw new ObfuscationConfigurationException("Failed to retrieve classpath elements attempting to setup compiler plugin.", e);
		}
    }
}
