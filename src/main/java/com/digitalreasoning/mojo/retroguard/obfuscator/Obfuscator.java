package com.digitalreasoning.mojo.retroguard.obfuscator;

import COM.rl.ant.RGpatchTask;
import COM.rl.ant.RetroGuardTask;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public abstract class Obfuscator {

    private File inJar;
    private File outJar;
    private File obfuscateLog;
    private File config;
    private File workDir;

    private Collection<File> dependentJars = new HashSet<File>();
    private Collection<File> dependentSpecs = new HashSet<File>();

    protected Obfuscator(File inJar, File outJar, File obfuscateLog, File config, File workDir) throws ObfuscationException
    {
        this.inJar = inJar;
        this.outJar = outJar;
        this.obfuscateLog = obfuscateLog;
        this.config = config;
        this.workDir = workDir;
        if(workDir.exists())
        {
            if(!workDir.isDirectory())
            {
                throw new ObfuscationException("workDir must be a directory");
            }
            else
            {
            // create directory here
            }
        }
    }

    public void setDependentJars(Collection<File> dependentJars) {
        this.dependentJars = dependentJars;
    }

    public void setDependentSpecs(Collection<File> dependentSpecs) {
        this.dependentSpecs = dependentSpecs;
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
            logInfo("Previously obfuscated jar still current, not running obfuscation...");
            return;
        }

        File outputJar = new File(workDir, "output.jar");

        logInfo("Obfuscating " + inJar);

        logDebug("Invoking RetroGuard with args:");
        logDebug("  " + generatedJar);
        logDebug("  " + outputJar);
        logDebug("  " + generatedSpec);
        logDebug("  " + obfuscateLog);

        try {
            Object retroGuardTask = setup();
            setInfile(retroGuardTask, generatedJar.getAbsolutePath());
            setOutfile(retroGuardTask, outputJar.getAbsolutePath());
            setLogfile(retroGuardTask, obfuscateLog.getAbsolutePath());
            setRgsfile(retroGuardTask, generatedSpec.getAbsolutePath());

            execute(retroGuardTask);
        } catch (BuildException e) {
            throw new ObfuscationException("RetroGuard invocation did not complete successfully.", e);
        } finally {
            teardown();
        }

        try {
            RGpatchTask patchTask = new RGpatchTask();
            patchTask.setInfile(outputJar.getAbsolutePath());
            patchTask.setOutfile(outJar.getAbsolutePath());
            patchTask.setRgsfile(obfuscateLog.getAbsolutePath());
            patchTask.setListfile(generateClassList().getAbsolutePath());

            patchTask.execute();
        } catch (BuildException e) {
            throw new ObfuscationException("Could not split out our obfuscated jar.", e);
        }
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
            for(File dependentSpec: dependentSpecs)
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
            FileWriter writer = null;
            try {
                writer = new FileWriter(combinedSpec);
                Files.copy(config, Charset.forName("UTF-8"), writer);
                for (File dependentSpec : dependentSpecs) {
                    Files.copy(dependentSpec, Charset.forName("UTF-8"), writer);
                }
            } catch (FileNotFoundException e) {
                throw new ObfuscationException("Failed to write spec file " + combinedSpec, e);
            } catch (IOException e) {
                throw new ObfuscationException("Failed to write spec file " + combinedSpec, e);
            } finally {
                try {
                    Closeables.close(writer, false);
                } catch (IOException e) {
                    throw new ObfuscationException("Closing spec file failed...", e);
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

    protected void execute(Object retroGuardTask) {
        ((RetroGuardTask)retroGuardTask).execute();
    }

    protected void teardown() {
        // do nothing
    }

    protected Object setup() throws ObfuscationException {
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
