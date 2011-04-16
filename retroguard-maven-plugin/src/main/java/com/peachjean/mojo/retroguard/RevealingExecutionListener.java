package com.peachjean.mojo.retroguard;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.peachjean.mojo.retroguard.ObfuscationConfiguration;
import com.peachjean.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.peachjean.mojo.retroguard.Utils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RevealingExecutionListener extends AbstractExecutionListener {

	@Requirement
    private List<ObfuscationMojoExecutionModifier> modifiers;

	/**
	 * Used to look up Artifacts in the remote repository.
	 *
	 * @parameter expression=
	 * "${component.org.apache.maven.artifact.factory.ArtifactFactory}"
	 * @required
	 * @readonly
	 */
	protected ArtifactFactory factory;

	/**
	 * Used to look up Artifacts in the remote repository.
	 *
	 * @component
	 */
	protected ArtifactResolver artifactResolver;

	/**
	 * List of Remote Repositories used by the resolver
	 *
	 * @parameter expression="${project.remoteProjectRepositories}"
	 * @readonly
	 * @required
	 */
	protected List<RemoteRepository> remoteRepositories;



	private ExecutionListener delegate;

	private MojoExecutor mojoExecutor;

    public RevealingExecutionListener() {
    }

    public RevealingExecutionListener(List<ObfuscationMojoExecutionModifier> modifiers, ExecutionListener delegate) {
        this.modifiers = modifiers;
        this.delegate = delegate;
    }

    public void setDelegate(ExecutionListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void mojoStarted(final ExecutionEvent event) {
        MavenSession session = event.getSession();
        if(Utils.OBFUSCATED_TYPE.equals(session.getCurrentProject().getPackaging()))
        {
	        Collection<ObfuscationMojoExecutionModifier> applicableModifiers = Collections2.filter(modifiers, new Predicate<ObfuscationMojoExecutionModifier>()
	        {
		        @Override
		        public boolean apply(ObfuscationMojoExecutionModifier input)
		        {
			        return Arrays.binarySearch(input.listApplicablePluginKeys(), event.getMojoExecution().getPlugin().getKey()) >= 0;
		        }
	        });
	        if(applicableModifiers.isEmpty())
	        {
		        return;
	        }
            MojoExecution mojoExecution = event.getMojoExecution();
            ObfuscationConfiguration obfuscationConfiguration = Utils.getObfuscationConfiguration(session);
            if(obfuscationConfiguration == null)
            {
                MojoExecution execution = new MojoExecution(session.getCurrentProject().getBuild().getPluginsAsMap().get("com.peachjean.mojo:retroguard-maven-plugin"), "init", "default-init");
                try {
                    mojoExecutor.execute(session, Collections.singletonList(execution), new ProjectIndex(session.getProjects()));
                } catch (LifecycleExecutionException e) {
                    throw new ObfuscationConfigurationException("Attempting to create obfuscation configuration...");
                }
            }
			for(ObfuscationMojoExecutionModifier mojoExecutionModifier : modifiers)
			{
				if(Arrays.binarySearch(mojoExecutionModifier.listApplicablePluginKeys(), mojoExecution.getPlugin().getKey()) > 0)
				{
					mojoExecutionModifier.modifyExecution(session, mojoExecution, obfuscationConfiguration);
				}
			}
        }
        delegate.mojoStarted(event);
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        delegate.mojoSucceeded(event);
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        delegate.mojoFailed(event);
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        delegate.forkStarted(event);
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        delegate.forkSucceeded(event);
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        delegate.forkFailed(event);
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        delegate.forkedProjectStarted(event);
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        delegate.forkedProjectSucceeded(event);
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        delegate.forkedProjectFailed(event);
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        delegate.projectDiscoveryStarted(event);
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        delegate.sessionStarted(event);
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        delegate.sessionEnded(event);
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        delegate.projectSkipped(event);
    }

	private boolean treatAsObfuscatedDependency(Artifact dependency) {
	    return Utils.OBFUSCATED_TYPE.equals(dependency.getType())
	            && ("compile".equals(dependency.getScope())
	                || "provided".equals(dependency.getScope())
	                || "runtime".equals(dependency.getScope()));
	}

    @Override
    public void projectStarted(ExecutionEvent event) {
	    if(Utils.OBFUSCATED_TYPE.equals(event.getSession().getCurrentProject().getPackaging()))
	    {
			MavenProject project = event.getSession().getCurrentProject();
			List<File> dependencySpecs = new ArrayList<File>();
			Map<String, File> unobfuscatedMapping = new HashMap<String, File>();
			Map<String, String> unobfuscatedIdMapping = new HashMap<String, String>();

			for(Artifact dependency: project.getArtifacts())
			{
				if(treatAsObfuscatedDependency(dependency))
				{
					VersionRange version = null;
					try {
						version = VersionRange.createFromVersionSpec(dependency.getVersion());
					} catch (InvalidVersionSpecificationException e) {
						throw new ObfuscationConfigurationException("Could not calculate version for " + dependency.toString(), e);
					}

					Artifact specArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.SPEC_TYPE, dependency.getClassifier(), dependency.getScope());
					Artifact unobfuscatedArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.UNOBFUSCATED_TYPE, dependency.getClassifier(), dependency.getScope());
					try {
						ArtifactRequest specRequest = new ArtifactRequest(RepositoryUtils.toArtifact(specArtifact), remoteRepositories, "retroguard");
						artifactResolver.resolveArtifact(event.getSession().getRepositorySession(), specRequest);
						ArtifactRequest unobfuscatedRequest = new ArtifactRequest(RepositoryUtils.toArtifact(unobfuscatedArtifact), remoteRepositories, "retroguard");
						artifactResolver.resolveArtifact(event.getSession().getRepositorySession(), unobfuscatedRequest);
					} catch (org.sonatype.aether.resolution.ArtifactResolutionException e)
					{
						throw new ObfuscationConfigurationException("Failed to locate retroguard artifacts for " + dependency.toString(), e);
					}

					dependencySpecs.add(specArtifact.getFile());
					unobfuscatedMapping.put(dependency.getFile().getPath(), unobfuscatedArtifact.getFile());
					unobfuscatedIdMapping.put(dependency.getId(), unobfuscatedArtifact.getId());
				}
			}

			Utils.initializeConfiguration(event.getSession(), dependencySpecs, unobfuscatedMapping, unobfuscatedIdMapping);
        }
        delegate.projectStarted(event);
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        delegate.projectSucceeded(event);
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        delegate.projectFailed(event);
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        delegate.mojoSkipped(event);
    }
}
