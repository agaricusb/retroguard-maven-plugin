package com.peachjean.mojo.retroguard;

import com.peachjean.mojo.retroguard.ObfuscationConfiguration;
import com.peachjean.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.peachjean.mojo.retroguard.Utils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.Collections;
import java.util.List;

public class RevealingExecutionListener extends AbstractExecutionListener {

    private List<ObfuscationMojoExecutionModifier> modifiers;
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
    public void mojoStarted(ExecutionEvent event) {
        MavenSession session = event.getSession();
        if(Utils.OBFUSCATED_TYPE.equals(session.getCurrentProject().getPackaging()))
        {
            MojoExecution mojoExecution = event.getMojoExecution();
            ObfuscationConfiguration obfuscationConfiguration = Utils.getObfuscationConfiguration(session);
            if(obfuscationConfiguration == null)
            {
                MojoExecution execution = new MojoExecution(session.getCurrentProject().getBuild().getPluginsAsMap().get("com.peachjean.mojo:retroguard-maven-plugin"), "init", "default-init");
                try {
                    mojoExecutor.execute(session, Collections.singletonList(execution), new ProjectIndex(session.getProjects()));
                } catch (LifecycleExecutionException e) {
                    throw new ObfuscationException("Attempting to create obfuscation configuration...");
                }
            }
			for(ObfuscationMojoExecutionModifier mojoExecutionModifier : modifiers)
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
