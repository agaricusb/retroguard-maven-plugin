package com.digitalreasoning.mojo.retroguard.obfuscator;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertArrayEquals;

public class RetroguardSpecFileTest {

	static final MessageDigest MESSAGE_DIGEST ;
	static {
		try {
			MESSAGE_DIGEST = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Help me!", e);
		}
	}

	File specFile = new File("src/test/data/spec.rgs");
	File rawSpecFile = new File("src/test/data/spec.rgs.raw");

	byte[] specDigest;
	byte[] rawSpecDigest;

	@Before
	public void setup() throws NoSuchAlgorithmException, IOException {
		specDigest = Files.getDigest(specFile, MESSAGE_DIGEST);
		rawSpecDigest = Files.getDigest(rawSpecFile, MESSAGE_DIGEST);
	}

	@Test
	public void testWriteRawSpec() throws Exception {
		RetroguardSpecFile rsf = new RetroguardSpecFile(specFile);
		final File outputFile = new File("target/testouput/retroguard.spec.rgs.raw");
		if(outputFile.exists()) {
			Files.deleteRecursively(outputFile);
		}
		Files.createParentDirs(outputFile);
		rsf.writeRawSpec(outputFile);
		byte[] outputDigest = Files.getDigest(outputFile, MESSAGE_DIGEST);
		assertArrayEquals(rawSpecDigest, outputDigest);
	}

	@Test
	public void testWriteSpec() throws Exception {
		final File outputFile = new File("target/testoutput/retroguard.spec.rgs");
		if(outputFile.exists()) {
			Files.deleteRecursively(outputFile);
		}
		Files.createParentDirs(outputFile);
		RetroguardSpecFile rsf = new RetroguardSpecFile("obfuscatedId", "unobfuscatedId", outputFile);
		rsf.writeSpec(rawSpecFile);
		byte[] outputDigest = Files.getDigest(outputFile, MESSAGE_DIGEST);
		assertArrayEquals(specDigest, outputDigest);
	}

	@Test
	public void testGetObfuscatedId() throws Exception {
		RetroguardSpecFile rsf = new RetroguardSpecFile(specFile);
		assert("obfuscatedId".equals(rsf.getObfuscatedId()));
	}

	@Test
	public void testGetUnobfuscatedId() throws Exception {
		RetroguardSpecFile rsf = new RetroguardSpecFile(specFile);
		assert("unobfuscatedId".equals(rsf.getUnobfuscatedId()));
	}
}
