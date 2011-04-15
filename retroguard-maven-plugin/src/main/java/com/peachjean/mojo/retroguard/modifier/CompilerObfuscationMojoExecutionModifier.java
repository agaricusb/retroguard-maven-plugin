package com.peachjean.mojo.retroguard.modifier;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.peachjean.mojo.retroguard.ObfuscationConfiguration;
import com.peachjean.mojo.retroguard.ObfuscationConfigurationException;
import com.peachjean.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.peachjean.mojo.retroguard.Utils;
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
    @Override
    public void modifyExecution(MavenSession session, MojoExecution mojoExecution, final ObfuscationConfiguration configuration) {
        if("org.apache.maven.plugins:maven-compiler-plugin".equals(mojoExecution.getPlugin().getKey()))
        {
            List<String> classpathElements;
            try {
                if(mojoExecution.getConfiguration().getChild("classpathElements") != null)
                {
                    Xpp3Dom classpathElementConfig = mojoExecution.getConfiguration().getChild("classpathElements");
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
	            final Map<String,File> unobfuscatedMapping = configuration.getUnobfuscatedMapping();
                Utils.augmentConfigurationList(mojoExecution.getConfiguration(), "classpathElements", classpathElements, new Function<String, String>() {
	                @Override
	                public String apply(String input)
	                {
		                return unobfuscatedMapping.get(input).getPath();
	                }
                });
            } catch (DependencyResolutionRequiredException e) {
                throw new ObfuscationConfigurationException("Failed to retrieve classpath elements attempting to setup compiler plugin.", e);
            }
        }

        // do nothing
    }
}
