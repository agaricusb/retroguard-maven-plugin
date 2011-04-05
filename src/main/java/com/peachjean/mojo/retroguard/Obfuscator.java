package com.peachjean.mojo.retroguard;

import COM.rl.ant.RetroGuardTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.BuildException;

import java.io.File;

public abstract class Obfuscator {

    File inJar;
    File outJar;
    File obfuscateLog;
    File config;

    protected Obfuscator(File inJar, File outJar, File obfuscateLog, File config) {
        this.inJar = inJar;
        this.outJar = outJar;
        this.obfuscateLog = obfuscateLog;
        this.config = config;
    }

    public void obfuscate() throws MojoExecutionException {
        if (!inJar.exists()) {
            throw new MojoExecutionException("Cannot find jar: " + inJar.getAbsolutePath());
        }

        if (!outJar.getParentFile().exists()) {
            if (!outJar.getParentFile().mkdirs()) {
                throw new MojoExecutionException("Could not create directory to place obfuscated jar in: " + outJar
                        .getParentFile().getAbsolutePath());
            }
        }

        if (!obfuscateLog.getParentFile().exists()) {
            if (!obfuscateLog.getParentFile().mkdirs()) {
                throw new MojoExecutionException("Could not create directory to place obfuscation log in: " + obfuscateLog
                        .getParentFile().getAbsolutePath());
            }
        }

        logInfo("Obfuscating " + inJar);

        logDebug("Invoking RetroGuard with args:");
        logDebug("  " + inJar);
        logDebug("  " + outJar);
        logDebug("  " + config);
        logDebug("  " + obfuscateLog);

        try {
            Object retroGuardTask = setup();
            setInfile(retroGuardTask, inJar.getAbsolutePath());
            setOutfile(retroGuardTask, outJar.getAbsolutePath());
            setLogfile(retroGuardTask, obfuscateLog.getAbsolutePath());
            if (config.exists()) {
                setRgsfile(retroGuardTask, config.getAbsolutePath());
            }

            execute(retroGuardTask);
        } catch (BuildException e) {
            throw new MojoExecutionException("RetroGuard invocation did not complete successfully.", e);
        } finally {
            teardown();
        }
    }

    protected void execute(Object retroGuardTask) {
        ((RetroGuardTask)retroGuardTask).execute();
    }

    protected void teardown() {
        // do nothing
    }

    protected Object setup() {
        return new RetroGuardTask();
    }

    protected void setInfile(Object task, String infile)
    {
        ((RetroGuardTask)task).setInfile(infile);
    }

    protected void setOutfile(Object task, String outfile)
    {
        ((RetroGuardTask)task).setOutfile(outfile);
    }

    protected void setLogfile(Object task, String logfile)
    {
        ((RetroGuardTask)task).setLogfile(logfile);
    }

    protected void setRgsfile(Object task, String rgsfile)
    {
        ((RetroGuardTask)task).setRgsfile(rgsfile);
    }

    protected abstract void logDebug(String message);

    protected abstract void logInfo(String message);

    protected abstract void logWarn(String message);

    protected abstract void logError(String message);
}
