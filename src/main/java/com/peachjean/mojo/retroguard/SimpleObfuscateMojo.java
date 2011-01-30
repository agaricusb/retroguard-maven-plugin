package com.peachjean.mojo.retroguard;

import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Goal which echoes a string.
 *
 * @goal simple
 * @requiresProject false
 */
public class SimpleObfuscateMojo extends AbstractObfuscateMojo {

	/**
	 * @parameter expression="${project}"
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * The source jar.
	 *
	 * @parameter expression="${retroguard.inJar}"
	 * @required true
	 */
	private File inJar;

	/**
	 * The output jar.
	 *
	 * @parameter expression="${retroguard.outJar}"
	 * @required true
	 */
	private File outJar;

	/**
	 * The retroguard log file.
	 *
	 * @parameter expression="${retroguard.log}" default-value=""
	 * @required true
	 */
	private File log;

	@Override
	protected void processArtifact(final File outJar, final File log) {
		// do nothing
	}

	@Override
	protected File getObfuscationLog() {
		if(log != null) {
			return log;
		} else if(project != null) {
			return new File(project.getBuild().getOutputDirectory(), "retroguard.log");
		} else {
			return new File("retroguard.log");
		}
	}

	@Override
	protected File getOutJar() {
		return outJar;
	}

	@Override
	protected File getInJar() {
		return inJar;
	}
}
