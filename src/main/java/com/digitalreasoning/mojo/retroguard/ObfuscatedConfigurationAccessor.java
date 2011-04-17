package com.digitalreasoning.mojo.retroguard;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObfuscatedConfigurationAccessor
{
	private static final String CONFIGURATION_SPECS_KEY = "obfuscation.Configuration.specs";
	private static final String CONFIGURATION_MAPPING_KEY = "obfuscation.Configuration.mapping";
	private static final String CONFIGURATION_IDMAPPING_KEY = "obfuscation.Configuration.idmapping";
	private static final String CONFIGURATION_ARTIFACTS_KEY = "obfuscation.Configuration.obfuscatedArtifacts";
	private static final String CONFIGURATION_UNARTIFACTS_KEY = "obfuscation.Configuration.unobfuscationArtifacts";


	/**
	 * Used to look up Artifacts in the remote repository.
	 *
	 * @parameter expression=
	 * "${component.org.apache.maven.artifact.factory.ArtifactFactory}"
	 * @required
	 * @readonly
	 */
	protected ArtifactFactory factory;

	/**
	 * Used to look up Artifacts in the remote repository.
	 *
	 * @component
	 */
	protected ArtifactResolver artifactResolver;

	private boolean isApplicable(String packaging)
	{
		return Utils.OBFUSCATED_JAR_TYPE.equals(packaging) || Utils.OBFUSCATED_WAR_TYPE.equals(packaging);
	}

	private boolean treatAsObfuscatedDependency(Dependency dependency) {
	    return (isApplicable(dependency.getType()))
	            && ("compile".equals(dependency.getScope())
	                || "provided".equals(dependency.getScope())
	                || "runtime".equals(dependency.getScope()));
	}

	public void resolveDependencies(MavenSession session)
	{
		String packaging = session.getCurrentProject().getPackaging();
		if(isApplicable(packaging))
		{
			MavenProject project = session.getCurrentProject();
			List<File> dependencySpecs = new ArrayList<File>();
			Map<String, File> unobfuscatedMapping = new HashMap<String, File>();
			Map<String, String> unobfuscatedIdMapping = new HashMap<String, String>();
			List<Artifact> obfuscatedArtifacts = new ArrayList<Artifact>();
			List<Artifact> unobfuscatedArtifacts = new ArrayList<Artifact>();

			for(Dependency dependency: project.getDependencies())
			{
				if(treatAsObfuscatedDependency(dependency))
				{
					VersionRange version = null;
					try
					{
						version = VersionRange.createFromVersionSpec(dependency.getVersion());
					} catch (InvalidVersionSpecificationException e)
					{
						throw new ObfuscationConfigurationException("Could not calculate version for " + dependency.toString(), e);
					}

					Artifact obfuscatedArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.OBFUSCATED_JAR_TYPE, dependency.getClassifier(), dependency.getScope());
					Artifact specArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.SPEC_TYPE, dependency.getClassifier(), dependency.getScope());
					Artifact unobfuscatedArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.UNOBFUSCATED_JAR_TYPE, dependency.getClassifier(), dependency.getScope());
					List<RemoteRepository> remoteRepositories = project.getRemoteProjectRepositories();
					ArtifactResult specResult;
					ArtifactResult obfuscatedResult;
					ArtifactResult unobfuscatedResult;
					try
					{
						ArtifactRequest obfuscatedRequest = new ArtifactRequest(RepositoryUtils.toArtifact(obfuscatedArtifact), remoteRepositories, "retroguard");
						obfuscatedResult = artifactResolver.resolveArtifact(session.getRepositorySession(), obfuscatedRequest);
						ArtifactRequest specRequest = new ArtifactRequest(RepositoryUtils.toArtifact(specArtifact), remoteRepositories, "retroguard");
						specResult = artifactResolver.resolveArtifact(session.getRepositorySession(), specRequest);
						ArtifactRequest unobfuscatedRequest = new ArtifactRequest(RepositoryUtils.toArtifact(unobfuscatedArtifact), remoteRepositories, "retroguard");
						unobfuscatedResult = artifactResolver.resolveArtifact(session.getRepositorySession(), unobfuscatedRequest);
					} catch (org.sonatype.aether.resolution.ArtifactResolutionException e)
					{
						throw new ObfuscationConfigurationException("Failed to locate retroguard artifacts for " + dependency.toString(), e);
					}

					dependencySpecs.add(specResult.getArtifact().getFile());
					unobfuscatedMapping.put(obfuscatedResult.getArtifact().getFile().getPath(), unobfuscatedResult.getArtifact().getFile());
					unobfuscatedIdMapping.put(obfuscatedArtifact.getId(), unobfuscatedArtifact.getId());
					obfuscatedArtifacts.add(RepositoryUtils.toArtifact(obfuscatedResult.getArtifact()));
					unobfuscatedArtifacts.add(RepositoryUtils.toArtifact(unobfuscatedResult.getArtifact()));
				}
			}

			initializeConfiguration(session, dependencySpecs, unobfuscatedMapping, unobfuscatedIdMapping, obfuscatedArtifacts, unobfuscatedArtifacts);
	    }

	}

	public ObfuscationConfiguration getObfuscationConfiguration(MavenSession session)
	{
		Map<String, Object> context = Utils.getRetroguardContext(session);
		if(!context.containsKey(CONFIGURATION_SPECS_KEY))
		{
			resolveDependencies(session);
		}
		return new ObfuscationConfiguration(
				(List<File>)context.get(CONFIGURATION_SPECS_KEY),
				(Map<String, File>)context.get(CONFIGURATION_MAPPING_KEY),
				(Map<String, String>)context.get(CONFIGURATION_IDMAPPING_KEY),
				(List<Artifact>)context.get(CONFIGURATION_ARTIFACTS_KEY),
				(List<Artifact>)context.get(CONFIGURATION_UNARTIFACTS_KEY));
	}


	public void initializeConfiguration(MavenSession session, List<File> dependencySpecs, Map<String, File> unobfuscatedMapping, Map<String, String> unobfuscatedIdMapping, List<Artifact> obfuscatedArtifacts, List<Artifact> unobfuscatedArtifacts)
	{
		Map<String, Object> context = Utils.getRetroguardContext(session);
		context.put(CONFIGURATION_SPECS_KEY, dependencySpecs);
		context.put(CONFIGURATION_MAPPING_KEY, unobfuscatedMapping);
		context.put(CONFIGURATION_IDMAPPING_KEY, unobfuscatedIdMapping);
		context.put(CONFIGURATION_ARTIFACTS_KEY, obfuscatedArtifacts);
		context.put(CONFIGURATION_UNARTIFACTS_KEY, unobfuscatedArtifacts);
	}

}
