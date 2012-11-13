package com.digitalreasoning.mojo.retroguard.obfuscator;

import COM.rl.obf.RetroGuardImpl;

import com.digitalreasoning.mojo.retroguard.Utils;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class MavenObfuscator {

    private Log log;
    private MavenProject project;
	private Class<RetroGuardImpl> retroGuardClass;

	private File inJar;
    private File outJar;
    private File obfuscateLog;
    private File config;
    private File workDir;

    private Collection<File> dependentJars = new HashSet<File>();
    private Collection<RetroguardSpecFile> dependentSpecs = new HashSet<RetroguardSpecFile>();
	
    public MavenObfuscator (File inJar, File outJar, File obfuscateLog, File config, File workDir, Log log, MavenProject project)
    throws ObfuscationException {
        this.inJar = inJar;
        this.outJar = outJar;
        this.obfuscateLog = obfuscateLog;
        this.config = config;
        this.workDir = workDir;

        if (workDir.exists() && !workDir.isDirectory())
        	throw new ObfuscationException("workDir must be a directory");

        this.log = log;
        this.project = project;
    }

    public Object setup()
    throws ObfuscationException {
        try {
            ClassRealm pluginRealm = (ClassRealm) Thread.currentThread().getContextClassLoader();
            ClassRealm childRealm = pluginRealm.createChildRealm(project.getId() + "-retroguard-task");
	        
            ArtifactFilter specFilter = new ArtifactFilter() {
		        @Override
		        public boolean include(Artifact artifact) {
			        return Utils.SPEC_TYPE.equals(artifact.getType());
		        }
	        };
	        
	        ArtifactFilter classpathFilter = new AndArtifactFilter( Arrays.asList(
	        		new ScopeArtifactFilter( Artifact.SCOPE_COMPILE ),
			        new ArtifactFilter() {
				        @Override
				        public boolean include (Artifact artifact) {
					        return artifact.isResolved() && !MavenObfuscator.this.getDependentJars().contains(artifact.getFile());
				        }
			        } ));
	        
	        Collection<RetroguardSpecFile> dependentSpecs = new ArrayList<RetroguardSpecFile>();
	        
	        for(Artifact artifact: project.getArtifacts()) {
	            if(classpathFilter.include(artifact))
	            {
                    childRealm.addURL(artifact.getFile().toURI().toURL());
                }
	            if(specFilter.include(artifact))
	            {
		            final RetroguardSpecFile specFile = new RetroguardSpecFile(artifact.getFile());
		            dependentSpecs.add(specFile);
	            }
            }
	        
            for(URL constituent: pluginRealm.getURLs()) {
                childRealm.addURL(constituent);
            }
            
            Thread.currentThread().setContextClassLoader(childRealm);
            return retroGuardClass = childRealm.loadClass(RetroGuardImpl.class.getName());
        } catch (Exception e) {
            throw new ObfuscationException("Could not setup retroguard classloader...", e);
        }
    }

    protected Collection<File> getDependentJars() {
		return dependentJars;
	}
    
    public void setDependentJars(Collection<File> dependentJars) {
        this.dependentJars = dependentJars;
    }
    
	public void setDependentSpecs(Collection<RetroguardSpecFile> dependentSpecs) {
        this.dependentSpecs = dependentSpecs;
    }

    private File generateClassList() throws ObfuscationException {
        File classList = new File(this.workDir, "classes.list");
        BufferedWriter classListWriter = null;
        try {
            classListWriter = new BufferedWriter(new FileWriter(classList));
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(inJar);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    classListWriter.write(entries.nextElement().getName());
                    classListWriter.newLine();
                }
            } finally {
                jarFile.close();
            }
        } catch (IOException e) {
            throw new ObfuscationException("Could not create classes.list.", e);
        } finally {
            Closeables.closeQuietly(classListWriter);
        }
        return classList;
    }

    protected File generateSpecFile() throws ObfuscationException {
        File combinedSpec = new File(this.workDir, "combined.spec");
        boolean regenerate = true;
        if(combinedSpec.exists())
        {
            regenerate = false;
            long specModified = combinedSpec.lastModified();
            for(RetroguardSpecFile dependentSpec: dependentSpecs)
            {
                if(dependentSpec.lastModified() > specModified)
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
            combinedSpec.delete();
            try {
                Files.createParentDirs(combinedSpec);
            } catch (IOException e) {
                throw new ObfuscationException("Could not create output directory...", e);
            }
	        for(RetroguardSpecFile specFile: dependentSpecs) {
		        try {
			        specFile.appendRawSpec(combinedSpec);
		        } catch (IOException e) {
			        throw new ObfuscationException("Could not generate combined spec file.");
		        }
	        }
        }
        return combinedSpec;
    }

    protected File generateInputJar(File combinedSpec) throws ObfuscationException {
        File combinedJar = new File(this.workDir, "combined.jar");
        boolean regenerate = !combinedJar.exists()
                || combinedSpec.lastModified() > combinedJar.lastModified()
                || inJar.lastModified() > combinedJar.lastModified();

        if(regenerate) {
            combinedJar.delete();
            JarOutputStream jarOut = null;
            try {
                jarOut = new JarOutputStream(new FileOutputStream(combinedJar));
                Set<String> addedEntries = new HashSet<String>();
                includeJar(jarOut, inJar, addedEntries);
                for(File dependentJar: this.dependentJars)
                {
                    includeJar(jarOut, dependentJar, addedEntries);
                }
            } catch (FileNotFoundException e) {
                throw new ObfuscationException("Failed to open combined jar for writing.", e);
            } catch (IOException e) {
                throw new ObfuscationException("Failed to write combined jar.", e);
            } finally {
                try {
                    Closeables.close(jarOut, false);
                } catch (IOException e) {
                    throw new ObfuscationException("Could not properly close combined jar file.", e);
                }
            }
        }
        return combinedJar;
    }

    private void includeJar(JarOutputStream jarOut, File dependentJar, Set<String> addedEntries) throws IOException {
        JarInputStream jarIn = null;
        try {
            jarIn = new JarInputStream(new FileInputStream(dependentJar));
            JarEntry entry = jarIn.getNextJarEntry();
            while(entry != null)
            {
                if(addedEntries.add(entry.getName())) {
                    jarOut.putNextEntry(entry);
                    IOUtils.copy(jarIn, jarOut);
                    jarOut.closeEntry();
                    jarIn.closeEntry();
                }
                entry = jarIn.getNextJarEntry();
            }
        } finally {
            Closeables.close(jarIn, false);
        }
    }
	
    public void obfuscate() throws ObfuscationException {
        if (!inJar.exists()) {
            throw new ObfuscationException("Cannot find jar: " + inJar.getAbsolutePath());
        }

        if (!outJar.getParentFile().exists()) {
            if (!outJar.getParentFile().mkdirs()) {
                throw new ObfuscationException("Could not create directory to place obfuscated jar in: " + outJar
                        .getParentFile().getAbsolutePath());
            }
        }

        if (!obfuscateLog.getParentFile().exists()) {
            if (!obfuscateLog.getParentFile().mkdirs()) {
                throw new ObfuscationException("Could not create directory to place obfuscation log in: " + obfuscateLog
                        .getParentFile().getAbsolutePath());
            }
        }


        File generatedSpec = generateSpecFile();
        File generatedJar = generateInputJar(generatedSpec);
        if (generatedJar.lastModified() < outJar.lastModified() && generatedSpec.lastModified() < outJar.lastModified())
        {
            log.info("Previously obfuscated jar still current, not running obfuscation...");
            return;
        }

        File outputJar = new File(workDir, "output.jar");

        log.info("Obfuscating " + inJar);

        log.debug("Invoking RetroGuard with args:");
        log.debug("  " + generatedJar);
        log.debug("  " + outputJar);
        log.debug("  " + generatedSpec);
        log.debug("  " + obfuscateLog);

        try {
        	setup();
            final Method method = retroGuardClass.getMethod(
            		"obfuscate", File.class, File.class, File.class, File.class );
            method.invoke( null, generatedJar, outJar, generatedSpec, obfuscateLog );
        } catch (InvocationTargetException e) {
        	throw new ObfuscationException(
        			"RetroGuard failed", e.getCause() );
        } catch (Exception e) {
            throw new ObfuscationException(
            		"reflective invocation of RetroGuard failed", e );
        } finally {
            teardown();
        }

        /*
        try {
            RGpatchTask patchTask = new RGpatchTask();
            patchTask.setInfile(outputJar.getAbsolutePath());
            patchTask.setOutfile(outJar.getAbsolutePath());
            patchTask.setRgsfile(obfuscateLog.getAbsolutePath());
            patchTask.setListfile(generateClassList().getAbsolutePath());

            patchTask.execute();
        } catch (BuildException e) {
            throw new ObfuscationException("Could not split out our obfuscated jar.", e);
        }*/
    }
    
    public void teardown() {
        Thread.currentThread().setContextClassLoader(Thread.currentThread().getContextClassLoader().getParent());
    }
}
