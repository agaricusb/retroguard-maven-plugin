package com.peachjean.mojo.retroguard;

import COM.rl.ant.RetroGuardTask;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MavenObfuscator extends Obfuscator {

    private Log log;
    private MavenProject project;
    private Class retroGuardTaskClass;

    public MavenObfuscator(File inJar, File outJar, File obfuscateLog, File config, Log log, MavenProject project) {
        super(inJar, outJar, obfuscateLog, config);
        this.log = log;
        this.project = project;
    }

    @Override
    public Object setup() {
        try {
            ClassRealm pluginRealm = (ClassRealm) Thread.currentThread().getContextClassLoader();
            ClassRealm childRealm = pluginRealm.createChildRealm("retroguard-task");
            for(Artifact artifact: (List<Artifact>) project.getCompileArtifacts())
            {
                childRealm.addURL(artifact.getFile().toURI().toURL());
            }
            for(URL constituent: pluginRealm.getURLs())
            {
                childRealm.addURL(constituent);
            }
            Thread.currentThread().setContextClassLoader(childRealm);
            retroGuardTaskClass = childRealm.loadClass(RetroGuardTask.class.getName());
            return retroGuardTaskClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void teardown() {
        Thread.currentThread().setContextClassLoader(Thread.currentThread().getContextClassLoader().getParent());
    }

    protected void callSetMethod(Object target, String method, String value)
    {
        try {
            retroGuardTaskClass.getMethod(method, String.class).invoke(target, value);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void execute(Object retroGuardTask) {
        try {
            retroGuardTaskClass.getMethod("execute").invoke(retroGuardTask);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void setInfile(Object task, String infile) {
        callSetMethod(task, "setInfile", infile);
    }

    @Override
    protected void setOutfile(Object task, String outfile) {
        callSetMethod(task, "setOutfile", outfile);
    }

    @Override
    protected void setLogfile(Object task, String logfile) {
        callSetMethod(task, "setLogfile", logfile);
    }

    @Override
    protected void setRgsfile(Object task, String rgsfile) {
        callSetMethod(task, "setRgsfile", rgsfile);
    }

    @Override
    protected void logDebug(String message) {
        log.debug(message);
    }

    @Override
    protected void logInfo(String message) {
        log.info(message);
    }

    @Override
    protected void logWarn(String message) {
        log.warn(message);
    }

    @Override
    protected void logError(String message) {
        log.error(message);
    }
}