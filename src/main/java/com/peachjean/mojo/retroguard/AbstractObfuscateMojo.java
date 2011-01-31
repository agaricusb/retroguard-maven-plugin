package com.peachjean.mojo.retroguard;

import COM.rl.ant.RetroGuardTask;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * Abstract mojo that does the actual obfuscation of a jar file.
 */
public abstract class AbstractObfuscateMojo extends AbstractMojo {
	/**
	 * The retroguard configuration.
	 *
	 * @parameter expression="${retroguard.config}" default-value="${project.basedir}/retroguard.conf"
	 * @required true
	 */
	private File config;

	public void execute()
			throws MojoExecutionException {

		File inJar = getInJar();
		File outJar = getOutJar();
		File log = getObfuscationLog();

		if (!inJar.exists()) {
			throw new MojoExecutionException("Cannot find jar: " + inJar.getAbsolutePath());
		}

		if (!outJar.getParentFile().exists()) {
			if (!outJar.getParentFile().mkdirs()) {
				throw new MojoExecutionException("Could not create directory to place obfuscated jar in: " + outJar
						.getParentFile().getAbsolutePath());
			}
		}

		if (!log.getParentFile().exists()) {
			if (!log.getParentFile().mkdirs()) {
				throw new MojoExecutionException("Could not create directory to place obfuscation log in: " + log
						.getParentFile().getAbsolutePath());
			}
		}

		getLog().info("Obfuscating " + inJar);

		getLog().debug("Invoking RetroGuard with args:");
		getLog().debug("  " + inJar);
		getLog().debug("  " + outJar);
		getLog().debug("  " + config);
		getLog().debug("  " + log);

		RetroGuardTask retroGuardTask = new RetroGuardTask();
		retroGuardTask.setInfile(inJar.getAbsolutePath());
		retroGuardTask.setOutfile(outJar.getAbsolutePath());
		retroGuardTask.setLogfile(log.getAbsolutePath());
		if (config.exists()) {
			retroGuardTask.setRgsfile(config.getAbsolutePath());
		}

		try {
//			RetroGuard.main(args); // doesn't work since we can't access the default package...
			retroGuardTask.execute();
		} catch (BuildException e) {
			throw new MojoExecutionException("RetroGuard invocation did not complete successfully.", e);
		}

		processArtifact(outJar, log);

		getLog().debug("Retroguard invoked successfully...");
	}

	protected abstract void processArtifact(File outJar, File log);

	protected abstract File getObfuscationLog();

	protected abstract File getOutJar();

	protected abstract File getInJar();
}
