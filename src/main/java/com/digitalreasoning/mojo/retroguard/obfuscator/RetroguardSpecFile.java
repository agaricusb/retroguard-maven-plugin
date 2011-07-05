package com.digitalreasoning.mojo.retroguard.obfuscator;

import com.google.common.io.*;

import java.io.*;
import java.nio.charset.Charset;

public class RetroguardSpecFile {
	static final int NUM_HEADER_LINES = 2;
	static final int OBFUSCATED_ID_INDEX = 0;
	static final int UNOBFUSCATED_ID_INDEX = 1;
	private final String[] headerLines;
	private final File file;

	public RetroguardSpecFile(File file) throws IOException {
		this.file = file;
		headerLines = Files.readLines(file, Charset.forName("UTF-8"), new LineProcessor<String[]>() {

			int i = 0;
			String[] values = new String[NUM_HEADER_LINES];

			@Override
			public boolean processLine(String line) throws IOException {
				if(i < NUM_HEADER_LINES) {
					values[i++] = line;
				}
				return i <= NUM_HEADER_LINES;
			}

			@Override
			public String[] getResult() {
				return values;
			}
		});
	}

	public RetroguardSpecFile(String obfuscatedId, String unobfuscatedId, File file) {
		this.headerLines = new String[NUM_HEADER_LINES];
		this.headerLines[OBFUSCATED_ID_INDEX] = obfuscatedId;
		this.headerLines[UNOBFUSCATED_ID_INDEX] = unobfuscatedId;
		this.file = file;
	}

	public void writeRawSpec(File rawSpec) throws IOException {
		Files.copy(new SpecInputSupplier(file), rawSpec, Charset.forName("UTF-8"));
	}

	public void appendRawSpec(File rawSpec) throws IOException {
		CharStreams.copy(new SpecInputSupplier(file), new RawSpecAppendOutputSupplier(rawSpec));
	}

	public void writeSpec(File rawSpec) throws IOException {
		Files.copy(rawSpec, Charset.forName("UTF-8"), new SpecOutputSupplier(file, headerLines));
	}

	public String getObfuscatedId() {
		return headerLines[OBFUSCATED_ID_INDEX];
	}

	public String getUnobfuscatedId() {
		return headerLines[UNOBFUSCATED_ID_INDEX];
	}

	public long lastModified() {
		return file.lastModified();
	}

	private static class SpecInputSupplier implements InputSupplier<BufferedReader> {

		private final File file;

		public SpecInputSupplier(File file) {
			this.file = file;
		}

		@Override
		public BufferedReader getInput() throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for(int i = 0; i <= NUM_HEADER_LINES; i++) {
				reader.readLine();
			}
			return reader;
		}
	}

	private static class SpecOutputSupplier implements OutputSupplier<BufferedWriter> {
		private final File file;
		private final String[] headerLines;

		public SpecOutputSupplier(File file, String[] headerLines) {
			this.file = file;
			this.headerLines = headerLines;
		}

		@Override
		public BufferedWriter getOutput() throws IOException {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(String headerLine: headerLines) {
				writer.write(headerLine);
				writer.newLine();
			}
			writer.write("--------------------------------------------------------------------------------");
			writer.newLine();
			return writer;
		}
	}

	private static class RawSpecAppendOutputSupplier implements OutputSupplier<BufferedWriter> {
		private File file;

		private RawSpecAppendOutputSupplier(File file) {
			this.file = file;
		}

		@Override
		public BufferedWriter getOutput() throws IOException {
			return new BufferedWriter(new FileWriter(file, true));
		}
	}
}