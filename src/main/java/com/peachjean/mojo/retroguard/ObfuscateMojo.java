package com.peachjean.mojo.retroguard;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;


/**
 * Goal which echoes a string.
 *
 * @goal obfuscate
 * @requiresProject true
 * @phase package
 */
public class ObfuscateMojo
		extends AbstractObfuscateMojo {

	/**
	 * Used for attaching the source jar to the project.
	 *
	 * @component
	 */
	private MavenProjectHelper projectHelper;

	/**
	 * @parameter expression="${project}"
	 * @readonly
	 * @required
	 */
	protected MavenProject project;

	/**
	 * Specifies whether or not to attach the map artifact to the project
	 *
	 * @parameter expression="${retroguard.attachMap}" default-value="true"
	 */
	private boolean attachMap;

	/**
	 * Specifies whether or not to attach the obfuscated jar to the project
	 *
	 * @parameter expression="${retroguard.attachObfuscatedJar}" default-value="true"
	 */
	private boolean attachObfuscatedJar;

	/**
	 * The classifier to use for the obfuscated jar.  Defaults to "obfuscated".
	 *
	 * @parameter expression="${retroguard.classifier}" default-value="obfuscated"
	 */
	private String classifier;

	/**
	 * The classifier of the source jar.
	 *
	 * @parameter expression="${retroguard.sourceClassifier}" default-value=""
	 * @required false
	 */
	private String sourceClassifier;

	/**
	 * The directory where the generated archive file will be put. Defaults to ${project.build.directory} specified
	 * in the
	 * pom or inherited from the super pom.
	 *
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	protected File outputDirectory;

	/**
	 * The filename to be used for the generated archive file. For the source:jar goal, "-sources" is appended to this
	 * filename. For the source:test-jar goal, "-test-sources" is appended. Defaults to ${project.build.finalName}
	 * specified in the pom or inherited from the super pom.
	 *
	 * @parameter expression="${project.build.finalName}"
	 * @required
	 */
	protected String finalName;

	private String useClassifier(String classifier, String prefix) {
		return StringUtils.isBlank(classifier) ? "" : prefix + classifier;
	}

	@Override
	protected void processArtifact(final File outJar, final File log) {

		if (attachMap) {
			projectHelper.attachArtifact(project, "retroguard.spec", "", log);
		}
		if (attachObfuscatedJar) {
			projectHelper.attachArtifact(project, project.getPackaging(), "obfuscated", outJar);
		}
	}

	@Override
	protected File getObfuscationLog() {
		return new File(outputDirectory, finalName + useClassifier(classifier, "-") + ".rgs");
	}

	@Override
	protected File getOutJar() {
		return new File(outputDirectory, finalName + useClassifier(classifier, "-") + ".jar");
	}

	@Override
	protected File getInJar() {
		return project.getArtifact().getFile();
	}

}
