package com.digitalreasoning.mojo.retroguard.modifier;

import com.digitalreasoning.mojo.retroguard.ObfuscationConfiguration;
import com.digitalreasoning.mojo.retroguard.ObfuscationMojoExecutionModifier;
import com.digitalreasoning.mojo.retroguard.Utils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class JarObfuscationMojoExecutionModifier implements ObfuscationMojoExecutionModifier
{

	private static final String[] APPLICABLE_KEYS = new String[] { "org.apache.maven.plugins:maven-jar-plugin" };

	@Override
	public String[] listApplicablePluginKeys()
	{
		return APPLICABLE_KEYS;
	}

	@Override
    public void modifyExecution(MavenSession session, MojoExecution mojoExecution, ObfuscationConfiguration configuration) {
        if(mojoExecution.getConfiguration().getChild("classifier") == null)
        {
            Xpp3Dom classifier = new Xpp3Dom("classifier");
            classifier.setValue(Utils.UNOBFUSCATED_CLASSIFIER);
            mojoExecution.getConfiguration().addChild(classifier);
        }
    }
}
