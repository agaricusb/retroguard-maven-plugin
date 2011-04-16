package com.peachjean.mojo.retroguard.mojo;

import com.peachjean.mojo.retroguard.ObfuscationConfiguration;
import com.peachjean.mojo.retroguard.Utils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Locates all obfuscated dependencies, resolves their spec files and unobfuscated jars if required.
 *
 * @goal init
 * @phase initialization
 * @requiresProject
 * @requiresDependencyResolution runtime
 */

public class InitMojo extends AbstractMojo {

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

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

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteProjectRepositories}"
     * @readonly
     * @required
     */
    protected List<RemoteRepository> remoteRepositories;

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession localSession;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

//        List<File> dependencySpecs = new ArrayList<File>();
//        Map<String, File> unobfuscatedMapping = new HashMap<String, File>();
//	    Map<String, String> unobfuscatedIdMapping = new HashMap<String, String>();
//
//	    for(Artifact dependency: project.getArtifacts())
//	    {
//	        if(treatAsObfuscatedDependency(dependency))
//	        {
//	            VersionRange version = null;
//	            try {
//	                version = VersionRange.createFromVersionSpec(dependency.getVersion());
//	            } catch (InvalidVersionSpecificationException e) {
//	                throw new MojoExecutionException("Could not calculate version for " + dependency.toString(), e);
//	            }
//
//	            Artifact specArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.SPEC_TYPE, dependency.getClassifier(), dependency.getScope());
//	            Artifact unobfuscatedArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.UNOBFUSCATED_TYPE, dependency.getClassifier(), dependency.getScope());
//	            try {
//		            ArtifactRequest specRequest = new ArtifactRequest(RepositoryUtils.toArtifact(specArtifact), remoteRepositories, "retroguard");
//		            artifactResolver.resolveArtifact(localSession.getRepositorySession(), specRequest);
//		            ArtifactRequest unobfuscatedRequest = new ArtifactRequest(RepositoryUtils.toArtifact(unobfuscatedArtifact), remoteRepositories, "retroguard");
//		            artifactResolver.resolveArtifact(localSession.getRepositorySession(), unobfuscatedRequest);
//	            } catch (org.sonatype.aether.resolution.ArtifactResolutionException e)
//	            {
//		            throw new MojoExecutionException("Failed to locate retroguard artifacts for " + dependency.toString(), e);
//	            }
//
//		        dependencySpecs.add(specArtifact.getFile());
//	            unobfuscatedMapping.put(dependency.getFile().getPath(), unobfuscatedArtifact.getFile());
//		        unobfuscatedIdMapping.put(dependency.getId(), unobfuscatedArtifact.getId());
//	        }
//	    }
//
//	    Utils.initializeConfiguration(localSession, dependencySpecs, unobfuscatedMapping, unobfuscatedIdMapping);
    }

	/**
     * Should eventually allow controlling with includes/excludes.
     * @param dependency
     * @return
     */
    private boolean treatAsObfuscatedDependency(Artifact dependency) {
        return Utils.OBFUSCATED_TYPE.equals(dependency.getType())
                && ("compile".equals(dependency.getScope())
                    || "provided".equals(dependency.getScope())
                    || "runtime".equals(dependency.getScope()));
    }
}
