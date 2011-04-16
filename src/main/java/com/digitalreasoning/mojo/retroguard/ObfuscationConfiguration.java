package com.digitalreasoning.mojo.retroguard;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ObfuscationConfiguration
{

	private List<File> dependencySpecs;
	private Map<String, File> unobfuscatedMapping;
	private Map<String, String> unobfuscatedIdMapping;

	public ObfuscationConfiguration(List<File> dependencySpecs, Map<String, File> unobfuscatedMapping, Map<String, String> unobfuscatedIdMapping)
	{
		this.dependencySpecs = dependencySpecs;
		this.unobfuscatedMapping = unobfuscatedMapping;
		this.unobfuscatedIdMapping = unobfuscatedIdMapping;
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
}
