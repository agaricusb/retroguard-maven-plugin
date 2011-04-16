package com.digitalreasoning.mojo.retroguard;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "obfuscating")
public class ObfuscatingMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private RevealingExecutionListener revealingExecutionListener;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        revealingExecutionListener.setDelegate(session.getRequest().getExecutionListener());
        session.getRequest().setExecutionListener(revealingExecutionListener);
        super.afterProjectsRead(session);
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        super.afterSessionStart(session);
    }
}
