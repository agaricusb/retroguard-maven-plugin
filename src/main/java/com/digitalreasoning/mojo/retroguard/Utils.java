package com.digitalreasoning.mojo.retroguard;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Utils {

    public static final String OBFUSCATED_TYPE = "obfuscated.jar";
    public static final String SPEC_TYPE = "retroguard-spec";
    public static final String UNOBFUSCATED_TYPE = "unobfuscated.jar";
	public static final String UNOBFUSCATED_CLASSIFIER = "unobfuscated";
    public static final String OBFUSCATED_EXTENSION = "obfuscated.jar";
    public static final String SPEC_EXTENSION = "rgs";
    public static final String GEN_SPEC_EXTENSION = "gen-spec";
    public static final String CONTEXT_SPEC_LIST = "specList";
    public static final String CONTEXT_UNOBFUSCATED_MAP = "unobfuscated";
	private static final String CONFIGURATION_SPECS_KEY = "obfuscation.Configuration.specs";
	private static final String CONFIGURATION_MAPPING_KEY = "obfuscation.Configuration.mapping";
	private static final String CONFIGURATION_IDMAPPING_KEY = "obfuscation.Configuration.idmapping";

	public static void augmentConfigurationList(Xpp3Dom configuration, String name, Iterable<String> values)
	{
		augmentConfigurationList(configuration, name, values, Functions.<String>identity());
	}

	public static <T> void augmentConfigurationList(Xpp3Dom configuration, String name, Iterable<? extends T> values, Function<T, String> converter)
	{
		augmentConfigurationList(configuration, name, values, Predicates.<T>alwaysTrue(), converter);
	}

    public static <T> void augmentConfigurationList(Xpp3Dom configuration, String name, Iterable<? extends T> values, Predicate<T> filter, Function<T, String> converter)
    {
        Xpp3Dom configList = configuration.getChild(name);
        if(configList == null)
        {
            configList = new Xpp3Dom(name);
            configuration.addChild(configList);
        }
        for(T value: values)
        {
	        if(filter.apply(value))
	        {
		        String convertedValue = converter.apply(value);
		        Xpp3Dom configValue = new Xpp3Dom(convertedValue);
		        configValue.setValue(convertedValue);
		        configList.addChild(configValue);
			}
        }
    }

//    public static void propagatePrivateFields(Object obj, Class<?> targetClass, String ... fieldNames) throws MojoExecutionException
//    {
//        for(String fieldName: fieldNames)
//        {
//            try {
//                Field target = targetClass.getDeclaredField(fieldName);
//                target.setAccessible(true);
//                Field source = obj.getClass().getDeclaredField(fieldName);
//                source.setAccessible(true);
//
//                target.set(obj, source.get(obj));
//            } catch (NoSuchFieldException e) {
//                throw new MojoExecutionException("Could not propagate field " + fieldName + ", this is probably a programming error.", e);
//            } catch (IllegalAccessException e) {
//                throw new MojoExecutionException("Could not propagate field " + fieldName + ", this is probably a programming error.", e);
//            }
//        }
//
//    }
//
//    public static List<String> replaceWithUnobfuscated(List<String> classpathElements, MavenSession session) {
//        Map<String, Object> pluginContext = getRetroguardContext(session);
//        final Map<String, File> unobfuscated =
//                (Map<String, File>) pluginContext.get(CONTEXT_UNOBFUSCATED_MAP);
//        return Lists.transform(classpathElements, new Function<String, String>() {
//            @Override
//            public String apply(String input) {
//                return unobfuscated.containsKey(input) ? unobfuscated.get(input).getPath() : input;
//            }
//        });
//    }

    public static URL getUnobfuscatedUrl(Artifact obfuscatedArtifact, MavenSession session) throws MalformedURLException {
	    return getObfuscationConfiguration(session).getUnobfuscatedMapping().get(obfuscatedArtifact.getFile().getPath()).toURI().toURL();
    }

    private static Map<String, Object> getRetroguardContext(MavenSession session)
    {
        PluginDescriptor desc = new PluginDescriptor();
        desc.setGroupId( "com.digitalreasoning.mojo" );
        desc.setArtifactId("retroguard-maven-plugin");

        return session.getPluginContext( desc, session.getCurrentProject());
    }

    public static ObfuscationConfiguration getObfuscationConfiguration(MavenSession session)
    {
	    Map<String, Object> context = getRetroguardContext(session);
	    return new ObfuscationConfiguration(
			    (List<File>)context.get(CONFIGURATION_SPECS_KEY),
			    (Map<String, File>)context.get(CONFIGURATION_MAPPING_KEY),
			    (Map<String, String>)context.get(CONFIGURATION_IDMAPPING_KEY));
    }

    public static File getArtifactFile( File basedir, String finalName, String classifier, String extension )
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

	public static void initializeConfiguration(MavenSession session, List<File> dependencySpecs, Map<String, File> unobfuscatedMapping, Map<String, String> unobfuscatedIdMapping)
	{
		Map<String, Object> context = getRetroguardContext(session);
		context.put(CONFIGURATION_SPECS_KEY, dependencySpecs);
		context.put(CONFIGURATION_MAPPING_KEY, unobfuscatedMapping);
		context.put(CONFIGURATION_IDMAPPING_KEY, unobfuscatedIdMapping);
	}
}
