package com.peachjean.mojo.retroguard;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
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
     * The retroguard configuration.
     *
     * @parameter expression="${retroguard.dependByDefault}" default-value="true"
     * @required true
     */
    private boolean dependByDefault = true;

//    private List<Dependency>

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
     * @parameter expression=
     * "${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List remoteRepositories;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepository;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        List<File> dependencySpecs = new ArrayList<File>();
        Map<String, File> unobfuscatedMapping = new HashMap<String, File>();

        resolveDependencies((List<Dependency>) project.getCompileDependencies(), dependencySpecs, unobfuscatedMapping);
        resolveDependencies((List<Dependency>) project.getRuntimeDependencies(), dependencySpecs, unobfuscatedMapping);

        this.getPluginContext().put(Utils.CONTEXT_SPEC_LIST, dependencySpecs);
        this.getPluginContext().put(Utils.CONTEXT_UNOBFUSCATED_MAP, unobfuscatedMapping);
    }

    private void resolveDependencies(List<Dependency> dependencies, List<File> dependencySpecs, Map<String, File> unobfuscatedMapping) throws MojoExecutionException {
        for(Dependency dependency: dependencies)
        {
            if(treatAsObfuscatedDependency(dependency))
            {
                VersionRange version = null;
                try {
                    version = VersionRange.createFromVersionSpec(dependency.getVersion());
                } catch (InvalidVersionSpecificationException e) {
                    throw new MojoExecutionException("Could not calculate version for " + dependency.toString(), e);
                }
                Artifact artifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.SPEC_TYPE, dependency.getClassifier(), dependency.getScope());
                Artifact obfArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, dependency.getType(), dependency.getClassifier(), dependency.getScope());
                Artifact unobfArtifact = factory.createDependencyArtifact(dependency.getGroupId(), dependency.getArtifactId(), version, Utils.UNOBFUSCATED_TYPE, dependency.getClassifier(), dependency.getScope());
                try {
                    artifactResolver.resolve(artifact, this.remoteRepositories, this.localRepository);
                    artifactResolver.resolve(obfArtifact, this.remoteRepositories, this.localRepository);
                    artifactResolver.resolve(unobfArtifact, this.remoteRepositories, this.localRepository);
                } catch (ArtifactResolutionException e) {
                    throw new MojoExecutionException("Failed to locate retroguard artifacts for " + dependency.toString(), e);
                } catch (ArtifactNotFoundException e) {
                    throw new MojoExecutionException("Failed to locate retroguard artifacts for " + dependency.toString(), e);
                }
                dependencySpecs.add(artifact.getFile());

                unobfuscatedMapping.put(obfArtifact.getFile().getPath(), unobfArtifact.getFile());
            }
        }
    }

    /**
     * Should eventually allow controlling with includes/excludes.
     * @param dependency
     * @return
     */
    private boolean treatAsObfuscatedDependency(Dependency dependency) {
        return Utils.OBFUSCATED_TYPE.equals(dependency.getType())
                && ("compile".equals(dependency.getScope())
                    || "provided".equals(dependency.getScope())
                    || "runtime".equals(dependency.getScope()));
    }
}
