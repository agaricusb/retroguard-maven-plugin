package com.digitalreasoning.mojo.retroguard;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ObfuscationConfiguration
{

	private List<File> dependencySpecs;
	private Map<String, File> unobfuscatedMapping;
	private Map<String, String> unobfuscatedIdMapping;
	private List<Artifact> obfuscatedArtifacts;
	private List<Artifact> unobfuscatedArtifacts;

	public ObfuscationConfiguration(List<File> dependencySpecs, Map<String, File> unobfuscatedMapping, Map<String, String> unobfuscatedIdMapping, List<Artifact> obfuscatedArtifacts, List<Artifact> unobfuscatedArtifacts)
	{
		this.dependencySpecs = dependencySpecs;
		this.unobfuscatedMapping = unobfuscatedMapping;
		this.unobfuscatedIdMapping = unobfuscatedIdMapping;
		this.obfuscatedArtifacts = obfuscatedArtifacts;
		this.unobfuscatedArtifacts = unobfuscatedArtifacts;
	}

	public List<File> getDependencySpecs()
	{
		return dependencySpecs;
	}

	public Map<String, File> getUnobfuscatedMapping()
	{
		return unobfuscatedMapping;
	}

	public Map<String, String> getUnobfuscatedIdMapping()
	{
		return unobfuscatedIdMapping;
	}

	public List<Artifact> getObfuscatedArtifacts()
	{
		return obfuscatedArtifacts;
	}

	public List<Artifact> getUnobfuscatedArtifacts()
	{
		return unobfuscatedArtifacts;
	}
}
