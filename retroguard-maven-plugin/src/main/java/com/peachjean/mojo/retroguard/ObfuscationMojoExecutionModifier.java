package com.peachjean.mojo.retroguard;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;

/**
 * Created by IntelliJ IDEA.
 * User: jbunting
 * Date: 4/8/11
 * Time: 1:15 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ObfuscationMojoExecutionModifier {

    void modifyExecution(MavenSession session, MojoExecution mojoExecution, ObfuscationConfiguration configuration);
}
