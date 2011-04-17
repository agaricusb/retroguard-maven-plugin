package com.digitalreasoning.mojo.retroguard;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RevealingExecutionListener extends AbstractExecutionListener {

	@Requirement
    private List<ObfuscationMojoExecutionModifier> modifiers;

	/**
	 * @component
	 */
	protected ObfuscatedDependencyResolver dependencyResolver;

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
        if(isApplicable(session.getCurrentProject().getPackaging()))
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
            ObfuscationConfiguration obfuscationConfiguration = dependencyResolver.getObfuscationConfiguration(session);
			for(ObfuscationMojoExecutionModifier mojoExecutionModifier : applicableModifiers)
			{
				mojoExecutionModifier.modifyExecution(session, mojoExecution, obfuscationConfiguration);
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

    @Override
    public void projectStarted(ExecutionEvent event) {
        delegate.projectStarted(event);
    }

	private boolean isApplicable(String packaging)
	{
		return Utils.OBFUSCATED_JAR_TYPE.equals(packaging) || Utils.OBFUSCATED_WAR_TYPE.equals(packaging);
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
