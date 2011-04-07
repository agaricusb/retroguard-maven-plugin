package com.peachjean.mojo.retroguard;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Utils {

    public static final String OBFUSCATED_TYPE = "obfuscated.jar";
    public static final String SPEC_TYPE = "retroguard-spec";
    public static final String UNOBFUSCATED_TYPE = "unobfuscated.jar";
    public static final String OBFUSCATED_EXTENSION = "obfuscated.jar";
    public static final String SPEC_EXTENSION = "rgs";
    public static final String GEN_SPEC_EXTENSION = "gen-spec";
    public static final String CONTEXT_SPEC_LIST = "specList";
    public static final String CONTEXT_UNOBFUSCATED_MAP = "unobfuscated";

    public static void propagatePrivateFields(Object obj, Class<?> targetClass, String ... fieldNames) throws MojoExecutionException
    {
        for(String fieldName: fieldNames)
        {
            try {
                Field target = targetClass.getDeclaredField(fieldName);
                target.setAccessible(true);
                Field source = obj.getClass().getDeclaredField(fieldName);
                source.setAccessible(true);

                target.set(obj, source.get(obj));
            } catch (NoSuchFieldException e) {
                throw new MojoExecutionException("Could not propagate field " + fieldName + ", this is probably a programming error.", e);
            } catch (IllegalAccessException e) {
                throw new MojoExecutionException("Could not propagate field " + fieldName + ", this is probably a programming error.", e);
            }
        }

    }

    public static List<String> replaceWithUnobfuscated(List<String> classpathElements, MavenSession session) {
        Map<String, Object> pluginContext = getRetroguardContext(session);
        final Map<String, File> unobfuscated =
                (Map<String, File>) pluginContext.get(CONTEXT_UNOBFUSCATED_MAP);
        return Lists.transform(classpathElements, new Function<String, String>() {
            @Override
            public String apply(String input) {
                return unobfuscated.containsKey(input) ? unobfuscated.get(input).getPath() : input;
            }
        });
    }

    public static URL getUnobfuscatedUrl(Artifact obfuscatedArtifact, MavenSession session) throws MalformedURLException {
        return ((Map<String, File>)getRetroguardContext(session).get(CONTEXT_UNOBFUSCATED_MAP)).get(obfuscatedArtifact.getFile().getPath()).toURI().toURL();
    }

    public static Map<String, Object> getRetroguardContext(MavenSession session)
    {
        PluginDescriptor desc = new PluginDescriptor();
        desc.setGroupId( "com.peachjean.mojo" );
        desc.setArtifactId( "retroguard-maven-plugin" );

        return session.getPluginContext( desc, session.getCurrentProject());
    }

    protected static File getArtifactFile( File basedir, String finalName, String classifier, String extension )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + "." + extension );
    }
}
