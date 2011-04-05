package com.peachjean.mojo.retroguard;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Locates all obfuscated dependencies, resolves their spec files and unobfuscated jars if required.
 *
 * @goal generateSpec
 * @phase generate-resources
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class GenerateSpecMojo extends AbstractMojo {

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" expression="${jar.finalName}" default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    /**
     * The retroguard configuration.
     *
     * @parameter expression="${retroguard.config}" default-value="${project.basedir}/retroguard.conf"
     * @required true
     */
    private File config;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // for each obfuscated dependency we need to get the spec file.  Then we need to check dates.  We will regenerate our spec file if ANY source files have changed
        File specFile = Utils.getArtifactFile(outputDirectory, finalName, classifier, Utils.GEN_SPEC_EXTENSION);
        Iterable<File> dependencySpecs = (Iterable<File>) getPluginContext().get(Utils.CONTEXT_SPEC_LIST);
        boolean regenerate = true;
        if(specFile.exists())
        {
            regenerate = false;
            long specModified = specFile.lastModified();
            for(File dependencySpec: dependencySpecs)
            {
                if(dependencySpec.lastModified() > specModified)
                {
                    regenerate = true;
                    break;
                }
            }
            if(config.lastModified() > specModified)
            {
                regenerate = true;
            }
        }

        if(regenerate) {
            specFile.delete();
            try {
                Files.createParentDirs(specFile);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not create output directory...", e);
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(specFile);
                Files.copy(config, Charset.forName("UTF-8"), writer);
                for (File dependencySpec : dependencySpecs) {
                    Files.copy(dependencySpec, Charset.forName("UTF-8"), writer);
                }
            } catch (FileNotFoundException e) {
                throw new MojoExecutionException("Failed to write spec file " + specFile, e);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write spec file " + specFile, e);
            } finally {
                try {
                    Closeables.close(writer, false);
                } catch (IOException e) {
                    throw new MojoExecutionException("Closing spec file failed...", e);
                }
            }
        }

    }
}
