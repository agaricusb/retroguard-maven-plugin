package com.peachjean.mojo.retroguard.modifier;

import com.peachjean.mojo.retroguard.ObfuscationConfiguration;
import com.peachjean.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.peachjean.mojo.retroguard.Utils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class JarObfuscationMojoExecutionModifier implements ObfuscationMojoExecutionModifier
{

    @Override
    public void modifyExecution(MavenSession session, MojoExecution mojoExecution, ObfuscationConfiguration configuration) {
        if("org.apache.maven.plugins:maven-jar-plugin".equals(mojoExecution.getPlugin().getKey())
                && mojoExecution.getConfiguration().getChild("classifier") == null)
        {
            Xpp3Dom classifier = new Xpp3Dom("classifier");
            classifier.setValue(Utils.UNOBFUSCATED_TYPE);
            mojoExecution.getConfiguration().addChild(classifier);
        }
    }
}
