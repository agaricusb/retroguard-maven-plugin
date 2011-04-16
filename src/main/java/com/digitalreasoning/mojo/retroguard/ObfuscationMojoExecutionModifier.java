package com.digitalreasoning.mojo.retroguard;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

/**
 * Interface implemented by lifecycle modifiers.  Multiple steps in the obfuscation lifecycle require small tweaks to
 * the default lifecycle.  Those tweaks should be implementations of this class that are declared as plexus components.
 */
public interface ObfuscationMojoExecutionModifier {

	/**
	 * Provides a list of plugins that this modifier should be applied to.  A null return value indicates "all".  The
	 * keys should be of the format returned by {@link org.apache.maven.model.Plugin#getKey Plugin.getKey()}.
	 * @return
	 */
	String[] listApplicablePluginKeys();

	/**
	 * Hook method that provides this modifier with the opportunity to tweak the MojoExecution.
	 * @param session
	 * @param mojoExecution
	 * @param configuration
	 */
    void modifyExecution(MavenSession session, MojoExecution mojoExecution, ObfuscationConfiguration configuration);
}
