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

    public static final String OBFUSCATED_JAR_TYPE = "obfuscated.jar";
	public static final String OBFUSCATED_WAR_TYPE = "obfuscated.war";
    public static final String SPEC_TYPE = "retroguard-spec";
    public static final String UNOBFUSCATED_JAR_TYPE = "unobfuscated.jar";
	public static final String UNOBFUSCATED_WAR_TYPE = "unobfuscated.war";
	public static final String UNOBFUSCATED_CLASSIFIER = "unobfuscated";
    public static final String SPEC_EXTENSION = "rgs";

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

    public static Map<String, Object> getRetroguardContext(MavenSession session)
    {
        PluginDescriptor desc = new PluginDescriptor();
        desc.setGroupId( "com.digitalreasoning.mojo" );
        desc.setArtifactId("retroguard-maven-plugin");

        return session.getPluginContext( desc, session.getCurrentProject());
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

}
