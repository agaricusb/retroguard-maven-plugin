package com.digitalreasoning.mojo.retroguard.obfuscator;

import COM.rl.obf.RetroGuardImpl;

import com.digitalreasoning.mojo.retroguard.Utils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MavenObfuscator {

    private Log log;
    private MavenProject project;
	private Class<RetroGuardImpl> retroGuardClass;

	private File inJar;
    private File outJar;
    private File obfuscateLog;
    private File config;
    private File workDir;
	
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
					        return artifact.isResolved();
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

        log.info("Obfuscating " + inJar);
        
        try {
        	setup();
            final Method method = retroGuardClass.getMethod(
            		"obfuscate", File.class, File.class, File.class, File.class );
            method.invoke( null, inJar, outJar, config, obfuscateLog );
        } catch (InvocationTargetException e) {
        	throw new ObfuscationException(
        			"RetroGuard failed", e.getCause() );
        } catch (Exception e) {
            throw new ObfuscationException(
            		"reflective invocation of RetroGuard failed", e );
        } finally {
            teardown();
        }
    }
    
    public void teardown() {
        Thread.currentThread().setContextClassLoader(Thread.currentThread().getContextClassLoader().getParent());
    }
}
