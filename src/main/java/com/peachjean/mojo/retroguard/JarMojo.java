package com.peachjean.mojo.retroguard;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.jar.AbstractJarMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Goal which echoes a string.
 *
 * @extendsPlugin jar
 * @goal jar
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class JarMojo extends org.apache.maven.plugin.jar.JarMojo {

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The retroguard configuration.
     *
     * @parameter expression="${retroguard.config}" default-value="${project.basedir}/retroguard.conf"
     * @required true
     */
    private File config;

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" expression="${jar.finalName}" default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {

        Utils.propagatePrivateFields(this, AbstractJarMojo.class, "finalName", "project");

        File jarFile = createArchive();

        String classifier = getClassifier();

        projectHelper.attachArtifact( project, Utils.UNOBFUSCATED_TYPE, classifier, jarFile );

        File outputDirectory = jarFile.getParentFile();
        File obfuscatedJarFile = Utils.getArtifactFile(outputDirectory, finalName, getClassifier(), Utils.OBFUSCATED_EXTENSION);
        File obfuscationLogFile = Utils.getArtifactFile(outputDirectory, finalName, getClassifier(), Utils.SPEC_EXTENSION);

        // for each obfuscated dependency we need to get the spec file.  Then we need to check dates.  We will regenerate our spec file if ANY source files have changed
        File specFile = Utils.getArtifactFile(outputDirectory, finalName, getClassifier(), Utils.GEN_SPEC_EXTENSION);
        boolean regenerate = false;
        if(specFile.exists())
        {
            long specModified = specFile.lastModified();
            for(File dependencySpec: (Iterable<File>) getPluginContext().get(Utils.CONTEXT_SPEC_LIST))
            {
                if(dependencySpec.lastModified() > specModified)
                {
                    regenerate = true;
                    break;
                }
            }
        }

        if(regenerate)
        {
            specFile.delete();
//            try {
//                FileWriter writer = new FileReader(specFile);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
        }

        MavenObfuscator obfuscator = new MavenObfuscator(jarFile, obfuscatedJarFile, obfuscationLogFile, specFile, getLog(), project);

        obfuscator.obfuscate();


        if ( classifier != null )
        {
            projectHelper.attachArtifact( getProject(), getType(), classifier, obfuscatedJarFile );
        }
        else
        {
            getProject().getArtifact().setFile( obfuscatedJarFile );
        }

//        projectHelper.attachArtifact(project, "obfuscated", classifier, obfuscatedJarFile);
        projectHelper.attachArtifact( project, Utils.SPEC_TYPE, classifier, obfuscationLogFile);
    }

    @Override
    protected String getType() {
        return Utils.OBFUSCATED_TYPE;
    }

}
