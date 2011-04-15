package com.peachjean.mojo.retroguard.modifier;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.peachjean.mojo.retroguard.ObfuscationConfiguration;
import com.peachjean.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.peachjean.mojo.retroguard.Utils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SurefireObfuscationMojoExecutionModifier implements ObfuscationMojoExecutionModifier
{

	private static final Predicate<Artifact> OBFUSCATED_ARTIFACT_FILTER = new Predicate<Artifact>() {

		@Override
		public boolean apply(Artifact input)
		{
			return Utils.OBFUSCATED_TYPE.equals(input.getType());
		}
	};

	List<String> applicablePlugins = Arrays.asList(new String[]{
            "org.apache.maven.plugins:maven-surefire-plugin",
            "org.apache.maven.plugins:maven-failsafe-plugin"
    });

    @Override
    public void modifyExecution(MavenSession session, MojoExecution mojoExecution, ObfuscationConfiguration configuration) {
        if(applicablePlugins.contains(mojoExecution.getPlugin().getKey()))
        {
            Utils.augmentConfigurationList(mojoExecution.getConfiguration(), "classpathDependencyExcludes", session.getCurrentProject().getArtifacts(), OBFUSCATED_ARTIFACT_FILTER,    new Function<Artifact, String>()
            {
	            @Override
	            public String apply(Artifact input)
	            {
		            return input.getId();
	            }
            });

	        final Map<String, String> idMapping = configuration.getUnobfuscatedIdMapping();
            Utils.augmentConfigurationList(mojoExecution.getConfiguration(), "additionalClasspathElements", session.getCurrentProject().getArtifacts(), OBFUSCATED_ARTIFACT_FILTER, new Function<Artifact, String>() {
                @Override
                public String apply(Artifact input) {
                    return idMapping.get(input.getId());
                }
            });
        }
        // do nothing
    }
}
